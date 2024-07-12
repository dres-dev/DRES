package dev.dres.run.validation.judged

import dev.dres.api.rest.types.evaluation.submission.ApiAnswerSet
import dev.dres.api.rest.types.evaluation.submission.ApiVerdictStatus
import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.api.rest.types.template.tasks.ApiTaskType
import dev.dres.data.model.submissions.*
import dev.dres.run.validation.interfaces.VoteValidator
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * An implementation of the [VoteValidator] that checks, if a submission is correct based on a manual judgement by a user followed by a public vote.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @author Ralph Gasser
 *
 * @version 2.1.0
 */
class BasicVoteValidator(
    taskTemplate: ApiTaskTemplate,
    store: TransientEntityStore,
    taskType: ApiTaskType,
    knownCorrectRanges: Collection<ItemRange> = emptyList(),
    knownWrongRanges: Collection<ItemRange> = emptyList(),
    private val minimumVotes: Int = defaultMinimimVotes,
    private val voteDifference: Int = defaultVoteDifference
) : BasicJudgementValidator(taskTemplate, store, taskType, knownCorrectRanges, knownWrongRanges), VoteValidator {

    constructor(taskTemplate: ApiTaskTemplate, store: TransientEntityStore, taskType: ApiTaskType, knownCorrectRanges: Collection<ItemRange> = emptyList(), knownWrongRanges: Collection<ItemRange> = emptyList(), parameters: Map<String, String>): this(
        taskTemplate, store, taskType,
        knownCorrectRanges, knownWrongRanges,
        parameters.getOrDefault(CONFIGURATION_MINIMUM_VOTES_KEY, "$defaultMinimimVotes").toIntOrNull() ?: defaultMinimimVotes,
        parameters.getOrDefault(CONFIGURATION_VOTE_DIFF_KEY, "$defaultVoteDifference").toIntOrNull() ?: defaultVoteDifference
    )

    init {
        require(this.minimumVotes > 0) { "Minimum vote count cannot be <= 0" }
    }

    companion object {
        private val defaultMinimimVotes = 5
        private val defaultVoteDifference = 1
        private const val CONFIGURATION_MINIMUM_VOTES_KEY = "VOTE.minimumVotes"
        private const val CONFIGURATION_VOTE_DIFF_KEY = "VOTE.voteDifference"
    }

    /** Internal queue of [AnswerSetId]s that pend voting. */
    private val submissionQueue = ConcurrentLinkedQueue<AnswerSetId>()

    /** Internal map that counts votes for [ApiVerdictStatus] for the current vote. */
    private val voteCountMap = ConcurrentHashMap<ApiVerdictStatus, Int>()

    /** Internal lock that mediates access to this [BasicVoteValidator]. */
    private val updateLock = ReentrantReadWriteLock()

    override val isActive: Boolean
        get() = this.updateLock.read { this.submissionQueue.isNotEmpty() }

    override val voteCount: Map<String, Int>
        get() = this.updateLock.read { this.voteCountMap.mapKeys { it.toString() } }

    /**
     * Places a vote for the current [AnswerSet].
     *
     * @param verdict The [ApiVerdictStatus] of the vote.
     */
    override fun vote(verdict: ApiVerdictStatus) = this.updateLock.write {
        if (verdict == ApiVerdictStatus.INDETERMINATE || verdict == ApiVerdictStatus.UNDECIDABLE){ //should not happen anyway but will be ignored in case it does
            return@write
        }

        val answerSet = this.current() ?: return@write
        this.voteCountMap[verdict] = 1 + this.voteCountMap.getOrDefault(verdict, 0)

        if (enoughVotes()){
            val finalVerdict = this.voteCountMap.entries.maxByOrNull { it.value }!!.key
            this.store.transactional {
                DbAnswerSet.filter { it.id eq answerSet.id }.firstOrNull()?.status = finalVerdict.toDb()
            }
            this.submissionQueue.poll()
            this.voteCountMap.clear()
        }
    }

    /**
     * Dequeues the next [DbAnswerSet] to vote for.
     *
     * @return [ApiAnswerSet] that requires voting, if exists.
     */
    override fun current(): ApiAnswerSet? = this.updateLock.read {
        val answerSetId = this.submissionQueue.firstOrNull()
        return this.store.transactional (true) { DbAnswerSet.filter { it.id eq answerSetId }.firstOrNull()?.toApi() }
    }

    /**
     * Places a verdict for the Submission identified by the given token. Inherits basic logic from parent class
     * but siphons undecidable entries to voting subsystem.
     *
     *
     * @param token The token used to identify the [DbSubmission].
     * @param verdict The verdict of the judge.
     */
    override fun judge(token: String, verdict: ApiVerdictStatus) = this.updateLock.write {
        val next = this.judgeInternal(token, verdict)
        if (verdict == ApiVerdictStatus.UNDECIDABLE) {
            this.submissionQueue.add(next)
        }
    }

    /**
     * Checks if enough votes have been gathered for the current round.
     *
     * @return True if enough votes have been gathered, false otherwise.
     */
    private fun enoughVotes() : Boolean {
        val sum = voteCountMap.values.sum()
        if (sum < minimumVotes) return false
        val max = voteCountMap.values.maxOrNull() ?: 0
        val others = sum - max
        return max - others >= voteDifference
    }
}

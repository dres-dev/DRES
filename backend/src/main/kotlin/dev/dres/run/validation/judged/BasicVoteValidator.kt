package dev.dres.run.validation.judged

import dev.dres.data.model.submissions.*
import dev.dres.run.validation.interfaces.VoteValidator
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.singleOrNull
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
 * @version 2.0.0
 */
class BasicVoteValidator(knownCorrectRanges: Collection<ItemRange> = emptyList(), knownWrongRanges: Collection<ItemRange> = emptyList(), private val minimumVotes: Int = defaultMinimimVotes, private val voteDifference: Int = defaultVoteDifference) : BasicJudgementValidator(knownCorrectRanges, knownWrongRanges), VoteValidator {

    constructor(knownCorrectRanges: Collection<ItemRange> = emptyList(), knownWrongRanges: Collection<ItemRange> = emptyList(), parameters: Map<String, String>): this(
        knownCorrectRanges, knownWrongRanges,
        parameters.getOrDefault("minimumVotes", "$defaultMinimimVotes").toIntOrNull() ?: defaultMinimimVotes,
        parameters.getOrDefault("voteDifference", "$defaultVoteDifference").toIntOrNull() ?: defaultVoteDifference
    )

    init {
        require(this.minimumVotes > 0) { "Minimum vote count cannot be <= 0" }
    }

    companion object {
        private val defaultMinimimVotes = 5
        private val defaultVoteDifference = 1
    }

    /** Internal queue of [AnswerSetId]s that pend voting. */
    private val submissionQueue = ConcurrentLinkedQueue<AnswerSetId>()

    /** Internal map that counts votes for [DbVerdictStatus] for the current vote. */
    private val voteCountMap = ConcurrentHashMap<DbVerdictStatus, Int>()

    /** Internal lock that mediates access to this [BasicVoteValidator]. */
    private val updateLock = ReentrantReadWriteLock()

    override val isActive: Boolean
        get() = this.updateLock.read { this.submissionQueue.isNotEmpty() }

    override val voteCount: Map<String, Int>
        get() = this.updateLock.read { this.voteCountMap.mapKeys { it.toString() } }

    /**
     * Places a vote for the current [DbAnswerSet].
     *
     * Requires an ongoing transaction!
     *
     * @param verdict The [DbVerdictStatus] of the vote.
     */
    override fun vote(verdict: DbVerdictStatus) = this.updateLock.write {
        if (verdict == DbVerdictStatus.INDETERMINATE || verdict == DbVerdictStatus.UNDECIDABLE){ //should not happen anyway but will be ignored in case it does
            return@write
        }

        val answerSet = this.current() ?: return@write
        this.voteCountMap[verdict] = 1 + this.voteCountMap.getOrDefault(verdict, 0)

        if (enoughVotes()){
            val finalVerdict = this.voteCountMap.entries.maxByOrNull { it.value }!!.key
            answerSet.status = finalVerdict
            this.submissionQueue.poll()
            this.voteCountMap.clear()
        }
    }

    /**
     * Dequeues the next [DbAnswerSet] to vote for.
     *
     * Requires an ongoing transaction.
     *
     * @return [DbAnswerSet] that requires voting.
     */
    override fun current(): DbAnswerSet? = this.updateLock.read {
        val answerSetId = this.submissionQueue.firstOrNull()
        return DbAnswerSet.filter { it.id eq answerSetId }.singleOrNull()
    }

    /**
     * Places a verdict for the [DbSubmission] identified by the given token. Inherits basic logic from parent class
     * but siphons undecidable entries to voting subsystem.
     *
     * Requires an ongoing transaction!
     *
     * @param token The token used to identify the [DbSubmission].
     * @param verdict The verdict of the judge.
     */
    override fun judge(token: String, verdict: DbVerdictStatus) = this.updateLock.write {
        val next = this.judgeInternal(token, verdict)
        if (verdict == DbVerdictStatus.UNDECIDABLE) {
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
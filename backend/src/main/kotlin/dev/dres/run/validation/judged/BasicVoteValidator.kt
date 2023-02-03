package dev.dres.run.validation.judged

import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.run.validation.interfaces.VoteValidator
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

class BasicVoteValidator(knownCorrectRanges: Collection<ItemRange> = emptyList(), knownWrongRanges: Collection<ItemRange> = emptyList(), val minimumVotes: Int = defaultMinimimVotes, val voteDifference: Int = defaultVoteDifference) : BasicJudgementValidator(knownCorrectRanges, knownWrongRanges), VoteValidator {

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

    private val submissionQueue = ConcurrentLinkedQueue<DbAnswerSet>()
    private val voteCountMap = ConcurrentHashMap<DbVerdictStatus, Int>()
    private val updateLock = ReentrantReadWriteLock()

    override val isActive: Boolean
        get() = submissionQueue.isNotEmpty()

    override val voteCount: Map<String, Int>
        get() = voteCountMap.mapKeys { it.toString() }

    override fun vote(status: DbVerdictStatus) = updateLock.write {
        if (status == DbVerdictStatus.INDETERMINATE || status == DbVerdictStatus.UNDECIDABLE){ //should not happen anyway but will be ignored in case it does
            return@write
        }

        val verdict = this.submissionQueue.firstOrNull() ?: return@write
        this.voteCountMap[status] = 1 + this.voteCountMap.getOrDefault(status, 0)

        if (enoughVotes()){
            val finalVerdict = this.voteCountMap.entries.maxByOrNull { it.value }!!.key
            verdict.status = finalVerdict
            this.submissionQueue.poll()
            this.voteCountMap.clear()
        }
    }

    private fun enoughVotes() : Boolean {
        val sum = voteCountMap.values.sum()
        if (sum < minimumVotes) return false
        val max = voteCountMap.values.maxOrNull() ?: 0
        val others = sum - max
        return max - others >= voteDifference
    }

    override fun nextSubmissionToVoteOn(): DbAnswerSet? = submissionQueue.firstOrNull() //TODO maybe add timeout mechanism?

    //siphon of undecidable submission from logic of super class
    override fun judge(token: String, status: DbVerdictStatus) {
        val verdict = super.processSubmission(token, status)
        when (status){
            DbVerdictStatus.CORRECT,
            DbVerdictStatus.WRONG -> verdict.status = status
            DbVerdictStatus.INDETERMINATE -> {}
            DbVerdictStatus.UNDECIDABLE -> this.submissionQueue.add(verdict)
        }
    }
}
package dev.dres.run.validation.judged

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
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
        require(minimumVotes > 0) {"minimum vote count cannot be <= 0"}
    }

    companion object {
        private val defaultMinimimVotes = 5
        private val defaultVoteDifference = 1
    }

    private val submissionQueue = ConcurrentLinkedQueue<Submission>()
    private val voteCountMap = ConcurrentHashMap<SubmissionStatus, Int>()
    private val updateLock = ReentrantReadWriteLock()

    override val isActive: Boolean
        get() = submissionQueue.isNotEmpty()

    override val voteCount: Map<String, Int>
        get() = voteCountMap.mapKeys { it.toString() }

    override fun vote(verdict: SubmissionStatus) = updateLock.write {

        if (verdict == SubmissionStatus.INDETERMINATE || verdict == SubmissionStatus.UNDECIDABLE){ //should not happen anyway but will be ignored in case it does
            return@write
        }

        val submission = submissionQueue.firstOrNull() ?: return@write

        voteCountMap[verdict] = 1 + voteCountMap.getOrDefault(verdict, 0)

        if (enoughVotes()){

            val finalVerdict = voteCountMap.entries.maxByOrNull { it.value }!!.key
            submission.status = finalVerdict

            submissionQueue.poll()
            voteCountMap.clear()

        }
    }

    private fun enoughVotes() : Boolean {
        val sum = voteCountMap.values.sum()
        if (sum < minimumVotes) return false
        val max = voteCountMap.values.maxOrNull() ?: 0
        return (sum - max) <= voteDifference
    }

    override fun nextSubmissionToVoteOn(): Submission? = submissionQueue.firstOrNull() //TODO maybe add timeout mechanism?

    //siphon of undecidable submission from logic of super class
    override fun judge(token: String, verdict: SubmissionStatus) {
        val submission = super.processSubmission(token, verdict)
        if (submission != null) {
            when(verdict){
                SubmissionStatus.CORRECT,
                SubmissionStatus.WRONG -> submission.status = verdict
                SubmissionStatus.INDETERMINATE -> {}
                SubmissionStatus.UNDECIDABLE -> submissionQueue.add(submission)
            }

        }
    }
}
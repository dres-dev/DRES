package dev.dres.run.validation.judged

import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.run.validation.interfaces.VoteValidator
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

class BasicVoteValidator(knownCorrectRanges: Collection<ItemRange> = emptyList(), knownWrongRanges: Collection<ItemRange> = emptyList()) : BasicJudgementValidator(knownCorrectRanges, knownWrongRanges), VoteValidator {

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
        return voteCountMap.values.sum() > 5 //TODO make configurable
    }

    override fun nextSubmissionToVoteOn(): Submission? = submissionQueue.firstOrNull() //TODO maybe add timeout mechanism?

    //siphon of undecidable submission from logic of super class
    override fun judge(token: String, verdict: SubmissionStatus) {
        val submission = super.doJudge(token, verdict)

        if (submission != null && verdict == SubmissionStatus.UNDECIDABLE) {
            submissionQueue.add(submission)
        }

    }
}
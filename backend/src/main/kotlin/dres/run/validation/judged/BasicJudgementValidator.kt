package dres.run.validation.judged

import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import dres.run.validation.interfaces.JudgementValidator
import java.util.*
import kotlin.collections.HashMap

/**
 * A validator class that checks, if a submission is correct based on a manual judgement by a user.
 *
 * TODO: Track these in the RunExecutor
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
class BasicJudgementValidator(override val callback: ((Submission) -> Unit)? = null): JudgementValidator { //TODO better name

    /** Internal queue that keeps track of all the [Submission]s in need of a verdict. */
    private val queue: Queue<Submission> = LinkedList()

    /** Internal map of all [Submission]s that have been retrieved by a judge and are pending a verdict. */
    private val waiting = HashMap<String, Submission>()

    /** Returns the number of [Submission]s that are currently pending a judgement. */
    override val pending: Int
        @Synchronized
        get() = this.queue.size + this.waiting.size

    /**
     * Enqueues a [Submission] with the internal judgment queue and updates its [SubmissionStatus]
     * to [SubmissionStatus.INDETERMINATE].
     *
     * @param submission The [Submission] to validate.
     * @return [SubmissionStatus] of the [Submission]
     */
    @Synchronized
    override fun validate(submission: Submission) {
        this.queue.offer(submission)
        submission.status = SubmissionStatus.INDETERMINATE
    }

    /**
     * Retrieves and returns the next element that requires a verdict from this [JudgementValidator]'
     * internal queue. If such an element exists, then the [Submission] is returned alongside a
     * unique token, that can be used to update the [Submission]'s [SubmissionStatus].
     *
     * @return Optional [Pair] containing a string token and the [Submission] that should be judged.
     */
    @Synchronized
    override fun next(queue: String): Pair<String, Submission>? {
        val next = this.queue.poll()
        return if (next != null) {
            val token = UUID.randomUUID().toString()
            this.waiting[token] = next
            Pair(token, next)
        } else {
            null
        }
    }

    /**
     * Places a verdict for the [Submission] identified by the given token.
     *
     * @param token The token used to identify the [Submission].
     * @param verdict The verdict of the judge.
     */
    @Synchronized
    override fun judge(token: String, verdict: SubmissionStatus) {
        require(this.waiting.containsKey(token)) { "This JudgementValidator does not contain a submission for the token '$token'." }
        val submission = this.waiting.getValue(token)
        submission.status = verdict
        this.callback?.invoke(submission) /* Invoke callback if any. */
    }

    /**
     * Clears this [JudgementValidator] and all the associated queues and maps.
     */
    @Synchronized
    fun clear() {
        this.waiting.clear()
        this.queue.clear()
    }
}
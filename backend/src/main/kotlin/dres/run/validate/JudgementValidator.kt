package dres.run.validate

import dres.data.model.competition.interfaces.TaskDescription
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus

interface JudgementValidator : SubmissionValidator {
    /** Returns the number of [Submission]s that are currently pending a judgement. */
    val pending: Int

    /**
     * Enqueues a [Submission] with the internal judgment queue.
     *
     * @param submission The [Submission] to validate.
     * @param task The [TaskDescription] that acts as a baseline for validation.
     *
     * @return [SubmissionStatus] of the [Submission]
     */
    override fun validate(submission: Submission): SubmissionStatus

    /**
     * Retrieves and returns the next element that requires a verdict from this [JudgementValidator]'
     * internal queue. If such an element exists, then the [Submission] is returned alongside a
     * unique token, that can be used to update the [Submission]'s [SubmissionStatus].
     *
     * @return Optional [Pair] containing a string token and the [Submission] that should be judged.
     */
    fun next(queue: String): Pair<String, Submission>?

    /**
     * Places a verdict for the [Submission] identified by the given token.
     *
     * @param token The token used to identify the [Submission].
     * @param verdict The verdict of the judge.
     */
    fun judge(token: String, verdict: SubmissionStatus)
}


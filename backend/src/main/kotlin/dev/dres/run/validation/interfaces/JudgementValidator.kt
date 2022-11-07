package dev.dres.run.validation.interfaces

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.Verdict
import dev.dres.data.model.submissions.VerdictStatus

/**
 * A [SubmissionValidator] that bases validation on human (manual) verdicts.
 *
 * This kind of [SubmissionValidator]  is inherently asynchronous.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.1.0
 */
interface JudgementValidator {
    /** unique id to identify the [JudgementValidator]*/
    val id: String

    /** The number of [Submission]s that are currently pending a judgement. */
    val pending: Int

    /** The number of [Submission]s which have not yet been presented to a judge */
    val open: Int

    /** Returns true, if this [JudgementValidator] has open [Submission]s. */
    val hasOpen: Boolean
        get() = open > 0

    /**
     * Retrieves and returns the next element that requires a verdict from this [JudgementValidator]'
     * internal queue. If such an element exists, then the [Submission] is returned alongside a
     * unique token, that can be used to update the [Submission]'s [VerdictStatus].
     *
     * @return Optional [Pair] containing a string token and the [Submission] that should be judged.
     */
    fun next(queue: String): Pair<String, Verdict>?

    /**
     * Places a verdict for the [Submission] identified by the given token.
     *
     * @param token The token used to identify the [Submission].
     * @param verdict The verdict of the judge.
     */
    fun judge(token: String, verdict: VerdictStatus)
}


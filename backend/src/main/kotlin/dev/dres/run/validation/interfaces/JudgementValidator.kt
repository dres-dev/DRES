package dev.dres.run.validation.interfaces

import dev.dres.api.rest.types.evaluation.submission.ApiAnswerSet
import dev.dres.api.rest.types.evaluation.submission.ApiVerdictStatus
import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.data.model.submissions.*

/**
 * A [AnswerSetValidator] that bases validation on human (manual) verdicts.
 *
 * This kind of [AnswerSetValidator]  is inherently asynchronous.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
interface JudgementValidator {
    /** unique id to identify the [JudgementValidator]*/
    val id: String

    /** The number of [DbSubmission]s that are currently pending a judgement. */
    val pending: Int

    /** The number of [DbSubmission]s which have not yet been presented to a judge */
    val open: Int

    /** Returns true, if this [JudgementValidator] has open [DbSubmission]s. */
    val hasOpen: Boolean
        get() = this.open > 0

    /** The template of the task this validator belongs to */
    val taskTemplate: ApiTaskTemplate

    /** The priority of this [JudgementValidator], higher priorities are represent by a higher number and consumers are expected to respect this */
    val priority: Int
        get() = 0
    /**
     * Retrieves and returns the next element that requires a verdict from this [JudgementValidator]'s internal queue.
     *
     * If such an element exists, then the [ApiAnswerSet] is returned alongside a unique token, that can be used to update the [ApiAnswerSet]'s [DbVerdictStatus].
     *
     * @return Optional [Pair] containing a string token and the [ApiAnswerSet] that should be judged.
     */
    fun next(): Pair<String, ApiAnswerSet>?

    /**
     * Places a verdict for the [Submission] identified by the given token.
     *
     * @param token The token used to identify the [Submission].
     * @param verdict The [DbVerdictStatus] assigned by the judge.
     */
    fun judge(token: String, verdict: ApiVerdictStatus)
}


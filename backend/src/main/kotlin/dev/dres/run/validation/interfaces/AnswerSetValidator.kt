package dev.dres.run.validation.interfaces

import dev.dres.data.model.submissions.AnswerSet
import dev.dres.data.model.submissions.DbSubmission

/**
 * A validator class that checks, if a [DbSubmission] is correct.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.1
 */
interface AnswerSetValidator {
    /**
     * Validates the [AnswerSet] and updates its [VerdictStatus].
     *
     * @param answerSet The [AnswerSet] to validate.
     */
    fun validate(answerSet: AnswerSet)

    /**
     * Indicates whether this [AnswerSetValidator] needs to defer the validation to some later point in time
     * or changes the status of a submission immediately
     */
    val deferring: Boolean
}
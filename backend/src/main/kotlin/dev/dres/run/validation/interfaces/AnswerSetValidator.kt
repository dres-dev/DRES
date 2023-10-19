package dev.dres.run.validation.interfaces

import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.DbSubmission

/**
 * A validator class that checks, if a [DbSubmission] is correct.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
interface AnswerSetValidator {
    /**
     * Indicates whether this [AnswerSetValidator] needs to defer the validation to some later point in time or changes the status of a submission immediately.
     */
    val deferring: Boolean

    /**
     * Validates the [DbAnswerSet] and updates its [DBVerdictStatus].
     *
     * Usually requires an ongoing transaction.
     *
     * @param answerSet The [DbAnswerSet] to validate.
     */
    fun validate(answerSet: DbAnswerSet)
}
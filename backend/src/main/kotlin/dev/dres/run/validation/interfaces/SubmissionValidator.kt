package dev.dres.run.validation.interfaces

import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.submissions.Submission

/**
 * A validator class that checks, if a [DbSubmission] is correct.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.1
 */
interface SubmissionValidator {
    /**
     * Validates the [DbSubmission] and updates its [DbVerdictStatus].
     *
     * @param submission The [DbSubmission] to validate.
     */
    fun validate(submission: Submission)

    /**
     * Indicates whether this [SubmissionValidator] needs to defer the validation to some later point in time
     * or changes the status of a submission immediately
     */
    val deferring: Boolean
}
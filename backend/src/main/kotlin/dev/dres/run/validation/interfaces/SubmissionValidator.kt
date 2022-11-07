package dev.dres.run.validation.interfaces

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.VerdictStatus

/**
 * A validator class that checks, if a [Submission] is correct.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.1
 */
interface SubmissionValidator {
    /**
     * Validates the [Submission] and updates its [VerdictStatus].
     *
     * @param submission The [Submission] to validate.
     */
    fun validate(submission: Submission)

    /**
     * Indicates whether this [SubmissionValidator] needs to defer the validation to some later point in time
     * or changes the status of a submission immediately
     */
    val deferring: Boolean
}
package dres.run.validate

import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus

/**
 * A validator class that checks, if  a submission is correct.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
interface SubmissionValidator {
    /**
     * Validates the [Submission] and returns its [SubmissionStatus].
     *
     * @param submission The [Submission] to validate.
     *
     * @return [SubmissionStatus] of the [Submission]
     */
    fun validate(submission: Submission): SubmissionStatus
}
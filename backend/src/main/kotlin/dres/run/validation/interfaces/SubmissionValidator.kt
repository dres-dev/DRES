package dres.run.validation.interfaces

import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus

/**
 * A validator class that checks, if  a submission is correct.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
interface SubmissionValidator {
    /** Callback function that is invoked everytime a [Submission] has been validated. */
    val callback: ((Submission) -> Unit)?

    /**
     * Validates the [Submission] and updates its [SubmissionStatus].
     *
     * @param submission The [Submission] to validate.
     */
    fun validate(submission: Submission)
}
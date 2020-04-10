package dres.run.validate

import dres.data.model.competition.interfaces.TaskDescription
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus

/**
 * A validator class that checks, if  a submission is correct.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
interface SubmissionValidator<T: TaskDescription> {
    /**
     * Validates the [Submission] and returns its [SubmissionStatus].
     *
     * @param submission The [Submission] to validate.
     * @param task The [TaskDescription] that acts as a baseline for validation.
     *
     * @return [SubmissionStatus] of the [Submission]
     */
    fun validate(submission: Submission, task: T): SubmissionStatus
}
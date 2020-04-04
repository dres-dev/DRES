package dres.run.validate

import dres.data.model.competition.TaskDescription
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus

/**
 *  Checks if a submission is correct
 */
interface SubmissionValidator<S : Submission, T: TaskDescription> {

    suspend fun validate(submission: S, task: T): SubmissionStatus

}
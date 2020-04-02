package dres.run.validate

import dres.api.rest.handler.SubmissionInfo
import dres.data.model.competition.TaskDescription
import dres.data.model.run.Submission

/**
 *  Checks if a submission is correct
 */
interface SubmissionValidator<S : Submission, T: TaskDescription> {

    fun validate(submission: S, task: T): SubmissionInfo.SubmissionStatus

}
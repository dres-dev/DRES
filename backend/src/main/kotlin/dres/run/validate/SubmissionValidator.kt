package dres.run.validate

import dres.data.model.competition.TaskDescription
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import kotlinx.coroutines.Deferred

/**
 *  Checks if a submission is correct
 */
interface SubmissionValidator<S : Submission, T: TaskDescription> {

    fun validate(submission: S, task: T): Deferred<SubmissionStatus>

}
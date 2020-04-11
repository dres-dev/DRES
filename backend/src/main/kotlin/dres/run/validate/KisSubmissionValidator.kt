package dres.run.validate

import dres.data.model.competition.TaskDescription
import dres.data.model.run.SubmissionStatus
import dres.data.model.run.VBSSubmission
import dres.utilities.TimeUtil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

class VisualKisSubmissionValidator : SubmissionValidator<VBSSubmission, TaskDescription.KisVisualTaskDescription> {

    //TODO framenumber not currently supported
    override fun validate(submission: VBSSubmission, task: TaskDescription.KisVisualTaskDescription): Deferred<SubmissionStatus> {
        val outer = TimeUtil.toMilliseconds(task.temporalRange)

        if (submission.start > submission.end) { //invalid submission
            return CompletableDeferred<SubmissionStatus>(SubmissionStatus.WRONG)
        }

        if (outer.first <= submission.start && outer.second >= submission.end) {
            return CompletableDeferred<SubmissionStatus>(SubmissionStatus.CORRECT)
        }

        return CompletableDeferred<SubmissionStatus>(SubmissionStatus.WRONG)
    }
}

class TextualKisSubmissionValidator : SubmissionValidator<VBSSubmission, TaskDescription.KisTextualTaskDescription> {

    //TODO framenumber not currently supported
    override fun validate(submission: VBSSubmission, task: TaskDescription.KisTextualTaskDescription): Deferred<SubmissionStatus> {
        val outer = TimeUtil.toMilliseconds(task.temporalRange)

        if (submission.start > submission.end) { //invalid submission
            return CompletableDeferred<SubmissionStatus>(SubmissionStatus.WRONG)
        }

        if (outer.first <= submission.start && outer.second >= submission.end) {
            return CompletableDeferred<SubmissionStatus>(SubmissionStatus.CORRECT)
        }

        return CompletableDeferred<SubmissionStatus>(SubmissionStatus.WRONG)
    }
}
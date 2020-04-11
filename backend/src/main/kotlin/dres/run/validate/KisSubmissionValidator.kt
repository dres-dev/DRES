package dres.run.validate

import dres.data.model.competition.TaskDescription
import dres.data.model.run.SubmissionStatus
import dres.data.model.run.VBSSubmission
import dres.utilities.TimeUtil

class VisualKisSubmissionValidator : SubmissionValidator<VBSSubmission, TaskDescription.KisVisualTaskDescription> {

    //TODO framenumber not currently supported
    override suspend fun validate(submission: VBSSubmission, task: TaskDescription.KisVisualTaskDescription): SubmissionStatus {
        val outer = TimeUtil.toMilliseconds(task.temporalRange)

        if (submission.start > submission.end) { //invalid submission
            return SubmissionStatus.WRONG
        }

        if (outer.first <= submission.start && outer.second >= submission.end) {
            return SubmissionStatus.CORRECT
        }

        return SubmissionStatus.WRONG
    }
}

class TextualKisSubmissionValidator : SubmissionValidator<VBSSubmission, TaskDescription.KisTextualTaskDescription> {

    //TODO framenumber not currently supported
    override suspend fun validate(submission: VBSSubmission, task: TaskDescription.KisTextualTaskDescription): SubmissionStatus {
        val outer = TimeUtil.toMilliseconds(task.temporalRange)

        if (submission.start > submission.end) { //invalid submission
            return SubmissionStatus.WRONG
        }

        if (outer.first <= submission.start && outer.second >= submission.end) {
            return SubmissionStatus.CORRECT
        }

        return SubmissionStatus.WRONG
    }
}
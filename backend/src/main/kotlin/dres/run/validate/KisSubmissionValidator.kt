package dres.run.validate

import dres.api.rest.handler.SubmissionInfo
import dres.data.model.competition.TaskDescription
import dres.data.model.run.KisSubmission
import dres.utilities.TimeUtil

class VisualKisSubmissionValidator : SubmissionValidator<KisSubmission, TaskDescription.KisVisualTaskDescription> {

    //TODO framenumber not currently supported
    override fun validate(submission: KisSubmission, task: TaskDescription.KisVisualTaskDescription): SubmissionInfo.SubmissionStatus {
        val outer = TimeUtil.toMilliseconds(task.temporalRange)

        if (submission.start > submission.end) { //invalid submission
            return SubmissionInfo.SubmissionStatus.WRONG
        }

        if (outer.first <= submission.start && outer.second >= submission.end) {
            return SubmissionInfo.SubmissionStatus.CORRECT
        }

        return SubmissionInfo.SubmissionStatus.WRONG
    }
}

class TextualKisSubmissionValidator : SubmissionValidator<KisSubmission, TaskDescription.KisTextualTaskDescription> {

    //TODO framenumber not currently supported
    override fun validate(submission: KisSubmission, task: TaskDescription.KisTextualTaskDescription): SubmissionInfo.SubmissionStatus {
        val outer = TimeUtil.toMilliseconds(task.temporalRange)

        if (submission.start > submission.end) { //invalid submission
            return SubmissionInfo.SubmissionStatus.WRONG
        }

        if (outer.first <= submission.start && outer.second >= submission.end) {
            return SubmissionInfo.SubmissionStatus.CORRECT
        }

        return SubmissionInfo.SubmissionStatus.WRONG
    }
}
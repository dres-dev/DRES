package dres.run.validation

import dres.data.model.competition.interfaces.MediaSegmentTaskDescription
import dres.data.model.competition.interfaces.TaskDescription
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import dres.run.validation.interfaces.SubmissionValidator
import dres.run.validation.judged.BasicJudgementValidator
import dres.utilities.TimeUtil

/**
 * A validator class that checks, if a submission is correct based on the target segment and the
 * temporal overlap of the [Submission] with the provided [MediaSegmentTaskDescription].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
class TemporalOverlapSubmissionValidator(private val task: MediaSegmentTaskDescription) : SubmissionValidator {

    /**
     * Validates a [Submission] based on the target segment and the temporal overlap of the
     * [Submission] with the [TaskDescription].
     *
     * @param submission The [Submission] to validate.
     */
    override fun validate(submission: Submission){
        submission.status = when {
            submission.start == null || submission.end == null -> SubmissionStatus.WRONG
            submission.start > submission.end -> SubmissionStatus.WRONG
            submission.item != task.item ->  SubmissionStatus.WRONG
            else -> {
                val outer = TimeUtil.toMilliseconds(this.task.temporalRange, this.task.item.fps)
                if ((outer.first <= submission.start && outer.second >= submission.start)  || (outer.first <= submission.end && outer.second >= submission.end)) {
                    SubmissionStatus.CORRECT
                } else {
                    SubmissionStatus.WRONG
                }
            }
        }
    }
}
package dres.run.validation

import dres.data.model.competition.interfaces.MediaSegmentTaskDescription
import dres.data.model.competition.interfaces.TaskDescription
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import dres.run.validation.interfaces.SubmissionValidator
import dres.utilities.TimeUtil

/**
 * A validator class that checks, if a submission is correct based on the target segment and the
 * temporal overlap of the [Submission] with the provided [MediaSegmentTaskDescription].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
class TemporalOverlapSubmissionValidator(private val task: MediaSegmentTaskDescription, override val callback: ((Submission) -> Unit)? = null) : SubmissionValidator {
    /**
     * Validates a [Submission] based on the target segment and the temporal overlap of the
     * [Submission] with the [TaskDescription]. TODO: Framenumber not currently supported
     *
     * @param submission The [Submission] to validate.
     */
    override fun validate(submission: Submission){
        if (submission.start == null || submission.end == null) {
            submission.status = SubmissionStatus.WRONG
        } else if (submission.start > submission.end) {
            submission.status = SubmissionStatus.WRONG
        } else if (submission.item != task.item) {
            submission.status = SubmissionStatus.WRONG
        } else {
            val outer = TimeUtil.toMilliseconds(this.task.temporalRange, this.task.item.fps)
            if (outer.first <= submission.start && outer.second >= submission.end) {
                submission.status = SubmissionStatus.CORRECT
            } else {
                submission.status = SubmissionStatus.WRONG
            }
        }

        /* Invoke callback if any. */
        this.callback?.invoke(submission)
    }
}
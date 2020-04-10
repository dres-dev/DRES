package dres.run.validate

import dres.data.model.competition.interfaces.MediaSegmentTaskDescription
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import dres.utilities.TimeUtil

/**
 * A validator class that checks, if a submission is correct based on the target segment and the
 * temporal overlap of the [Submission] with the provided [MediaSegmentTaskDescription].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
object TemporalOverlapSubmissionValidator : SubmissionValidator<MediaSegmentTaskDescription> {
    /**
     * Validates a [Submission] based on the target segment and the temporal overlap of the
     * [Submission] with the [TaskDescription]. TODO: Framenumber not currently supported
     *
     * @param submission The [Submission] to validate.
     * @param task The [TaskDescription] that acts as a baseline for validation.
     *
     * @return [SubmissionStatus] of the [Submission]
     */
    override fun validate(submission: Submission, task: MediaSegmentTaskDescription): SubmissionStatus {
        if (submission.start == null || submission.end == null) return SubmissionStatus.WRONG // Invalid
        if (submission.start > submission.end) return SubmissionStatus.WRONG // Invalid


        val outer = TimeUtil.toMilliseconds(task.temporalRange)
        if (outer.first <= submission.start && outer.second >= submission.end) {
            return SubmissionStatus.CORRECT
        }
        return SubmissionStatus.WRONG
    }
}
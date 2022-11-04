package dev.dres.run.validation

import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.time.TemporalRange
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.SubmissionType
import dev.dres.run.validation.interfaces.SubmissionValidator

/** */
typealias TransientMediaSegment = Pair<MediaItem,TemporalRange>

/**
 * A [SubmissionValidator] class that checks, if a submission is correct based on the target segment and the
 * temporal overlap of the [Submission] with the provided [TransientMediaSegment].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.1
 */
class TemporalOverlapSubmissionValidator(private val targetSegment: TransientMediaSegment) : SubmissionValidator {

    override val deferring: Boolean = false

    /**
     * Validates a [Submission] based on the target segment and the temporal overlap of the
     * [Submission] with the [TaskTemplate].
     *
     * @param submission The [Submission] to validate.
     */
    override fun validate(submission: Submission) {
        if (submission.type != SubmissionType.TEMPORAL) {
            submission.status = SubmissionStatus.WRONG
            return
        }

        if (submission.start > submission.end) {
            submission.status = SubmissionStatus.WRONG
            return
        }

        /* Perform item validation. */
        if (submission.item != this.targetSegment.first) {
            submission.status = SubmissionStatus.WRONG
            return
        }

        /* Perform temporal validation. */
        val outer = this.targetSegment.second.toMilliseconds()
        if ((outer.first <= submission.start && outer.second >= submission.start)  || (outer.first <= submission.end && outer.second >= submission.end)) {
            submission.status = SubmissionStatus.CORRECT
        } else {
            submission.status = SubmissionStatus.WRONG
        }
    }
}
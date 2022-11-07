package dev.dres.run.validation

import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.data.model.submissions.VerdictType
import dev.dres.run.validation.interfaces.SubmissionValidator
import kotlinx.dnq.query.asSequence

/**
 * A [SubmissionValidator] class that checks, if a submission is correct based on the target segment and the
 * complete containment of the [Submission] within the provided [MediaSegmentTaskDescription].
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 1.1.0
 */
class TemporalContainmentSubmissionValidator(private val targetSegment: TransientMediaSegment) : SubmissionValidator {

    override val deferring: Boolean
        get() = false

    /**
     * Validates a [Submission] based on the target segment and the temporal overlap of the
     * [Submission] with the [TaskTemplate].
     *
     * @param submission The [Submission] to validate.
     */
    override fun validate(submission: Submission) {
        submission.verdicts.asSequence().forEach { verdict ->
            /* Perform sanity checks. */
            if (verdict.type != VerdictType.TEMPORAL) {
                verdict.status = VerdictStatus.WRONG
                return@forEach
            }

            val start = verdict.start
            val end = verdict.end
            val item = verdict.item
            if (item == null || start == null || end == null || start > end) {
                verdict.status = VerdictStatus.WRONG
                return@forEach

            }

            /* Perform item validation. */
            if (verdict.item != this.targetSegment.first) {
                verdict.status = VerdictStatus.WRONG
                return@forEach
            }

            /* Perform temporal validation. */
            val outer = this.targetSegment.second.toMilliseconds()
            if (outer.first <= start && outer.second >= end) {
                verdict.status = VerdictStatus.CORRECT
            } else {
                verdict.status = VerdictStatus.WRONG
            }
        }
    }
}
package dev.dres.run.validation

import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.time.TemporalRange
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.submissions.DbAnswerType
import dev.dres.run.validation.interfaces.SubmissionValidator
import kotlinx.dnq.query.asSequence

/** */
typealias TransientMediaSegment = Pair<DbMediaItem,TemporalRange>

/**
 * A [SubmissionValidator] class that checks, if a submission is correct based on the target segment and the
 * temporal overlap of the [DbSubmission] with the provided [TransientMediaSegment].
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 1.1.0
 */
class TemporalOverlapSubmissionValidator(private val targetSegment: TransientMediaSegment) : SubmissionValidator {

    override val deferring: Boolean = false

    /**
     * Validates a [DbSubmission] based on the target segment and the temporal overlap of the
     * [DbSubmission] with the [DbTaskTemplate].
     *
     * @param submission The [DbSubmission] to validate.
     */
    override fun validate(submission: DbSubmission) {
        submission.verdicts.asSequence().forEach { verdict ->
            /* Perform sanity checks. */
            if (verdict.type != DbAnswerType.TEMPORAL) {
                verdict.status = DbVerdictStatus.WRONG
                return@forEach
            }

            val start = verdict.start
            val end = verdict.end
            val item = verdict.item
            if (item == null || start == null || end == null || start > end) {
                verdict.status = DbVerdictStatus.WRONG
                return@forEach
            }

            /* Perform item validation. */
            if (verdict.item != this.targetSegment.first) {
                verdict.status = DbVerdictStatus.WRONG
                return@forEach
            }

            /* Perform temporal validation. */
            val outer = this.targetSegment.second.toMilliseconds()
            if ((outer.first <= start && outer.second >= start)  || (outer.first <= end && outer.second >= end)) {
                verdict.status = DbVerdictStatus.CORRECT
            } else {
                verdict.status = DbVerdictStatus.WRONG
            }
        }
    }
}
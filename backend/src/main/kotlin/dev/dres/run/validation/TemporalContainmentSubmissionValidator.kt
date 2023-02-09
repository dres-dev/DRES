package dev.dres.run.validation

import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.submissions.DbAnswerType
import dev.dres.data.model.submissions.Submission
import dev.dres.run.validation.interfaces.SubmissionValidator
import kotlinx.dnq.query.asSequence

/**
 * A [SubmissionValidator] class that checks, if a submission is correct based on the target segment and the
 * complete containment of the [DbSubmission] within the provided [MediaSegmentTaskDescription].
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 1.1.0
 */
class TemporalContainmentSubmissionValidator(private val targetSegment: TransientMediaSegment) : SubmissionValidator {

    override val deferring: Boolean
        get() = false

    /**
     * Validates a [DbSubmission] based on the target segment and the temporal overlap of the
     * [DbSubmission] with the [DbTaskTemplate].
     *
     * @param submission The [DbSubmission] to validate.
     */
    override fun validate(submission: Submission) {
        submission.answerSets().forEach { answerSet ->

            answerSet.answers().forEach { answer ->

                /* Perform sanity checks. */
                if (answer.type != DbAnswerType.TEMPORAL) {
                    answerSet.status(DbVerdictStatus.WRONG)
                    return@forEach
                }

                val start = answer.start
                val end = answer.end
                val item = answer.item
                if (item == null || start == null || end == null || start > end) {
                    answerSet.status(DbVerdictStatus.WRONG)
                    return@forEach

                }

                /* Perform item validation. */
                if (answer.item != this.targetSegment.first) {
                    answerSet.status(DbVerdictStatus.WRONG)
                    return@forEach
                }

                /* Perform temporal validation. */
                val outer = this.targetSegment.second.toMilliseconds()
                if (outer.first <= start && outer.second >= end) {
                    answerSet.status(DbVerdictStatus.CORRECT)
                } else {
                    answerSet.status(DbVerdictStatus.WRONG)
                }

            }


        }
    }
}
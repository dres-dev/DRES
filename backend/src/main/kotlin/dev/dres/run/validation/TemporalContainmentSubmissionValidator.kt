package dev.dres.run.validation

import dev.dres.data.model.submissions.*
import dev.dres.data.model.template.task.DbTaskTemplate
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
        submission.answerSets().forEach outer@{ answerSet ->

            answerSet.answers().forEach inner@{ answer ->

                /* Perform sanity checks. */
                if (answer.type() != AnswerType.TEMPORAL) {
                    answerSet.status(VerdictStatus.WRONG)
                    return@inner
                }

                val start = answer.start
                val end = answer.end
                val item = answer.item
                if (item == null || start == null || end == null || start > end) {
                    answerSet.status(VerdictStatus.WRONG)
                    return@inner

                }

                /* Perform item validation. */
                if (answer.item?.mediaItemId != this.targetSegment.first.mediaItemId) {
                    answerSet.status(VerdictStatus.WRONG)
                    return@inner
                }

                /* Perform temporal validation. */
                val outer = this.targetSegment.second.toMilliseconds()
                if (outer.first <= start && outer.second >= end) {
                    answerSet.status(VerdictStatus.CORRECT)
                } else {
                    answerSet.status(VerdictStatus.WRONG)
                }

            }


        }
    }
}
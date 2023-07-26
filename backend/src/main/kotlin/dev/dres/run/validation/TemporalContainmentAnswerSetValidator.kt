package dev.dres.run.validation

import dev.dres.data.model.submissions.*
import dev.dres.run.validation.interfaces.AnswerSetValidator
import kotlinx.dnq.query.iterator

/**
 * A [AnswerSetValidator] class that checks, if a submission is correct based on the target segment and the
 * complete containment of the [DbSubmission] within the provided [MediaSegmentTaskDescription].
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
class TemporalContainmentAnswerSetValidator(private val targetSegment: TransientMediaSegment) : AnswerSetValidator {

    override val deferring: Boolean
        get() = false

    /**
     * Validates the [DbAnswerSet] and updates its [DBVerdictStatus].
     *
     * Usually requires an ongoing transaction.
     *
     * @param answerSet The [DbAnswerSet] to validate.
     */
    override fun validate(answerSet: DbAnswerSet) {

        /* Basically, we assume that the DBAnswerSet is wrong. */
        answerSet.status = DbVerdictStatus.WRONG

        /* Now we check all the answers. */
        for (answer in answerSet.answers) {
            /* Perform sanity checks. */
            val item = answer.item
            val start = answer.start
            val end = answer.end
            if (answer.type != DbAnswerType.TEMPORAL || item == null || start == null || end == null || start > end) {
                return
            }

            /* Perform item validation. */
            if (item.mediaItemId != this.targetSegment.first.mediaItemId) {
                return
            }

            /* Perform temporal validation. */
            val outer = this.targetSegment.second.toMilliseconds()
            if (outer.first > start || outer.second < end) {
                return
            }
        }

        /* If code reaches this point, the [DbAnswerSet] is correct. */
        answerSet.status = DbVerdictStatus.CORRECT
    }
}
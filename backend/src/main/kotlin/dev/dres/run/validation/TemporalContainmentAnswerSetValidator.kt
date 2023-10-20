package dev.dres.run.validation

import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.time.TemporalRange
import dev.dres.data.model.submissions.*
import dev.dres.run.validation.interfaces.AnswerSetValidator
import kotlinx.dnq.query.FilteringContext.isEmpty
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.iterator
import kotlinx.dnq.query.size

typealias TransientMediaSegment = Pair<MediaItem, TemporalRange>

/**
 * A [AnswerSetValidator] class that checks, if a submission is correct based on the target segment and the
 * complete containment of the [DbSubmission] within the provided [MediaSegmentTaskDescription].
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
class TemporalContainmentAnswerSetValidator(private val targetSegments: Collection<TransientMediaSegment>) :
    AnswerSetValidator {

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

        /* If there are answers, there could be a correct one */
        var correct = answerSet.answers.size() > 0

        /* Now we check all the answers. If an incorrect one is found, we break */
        for (answer in answerSet.answers) {
            /* Perform sanity checks. */
            val item = answer.item
            val start = answer.start
            val end = answer.end
            if (answer.type != DbAnswerType.TEMPORAL || item == null || start == null || end == null || start > end) {
                correct = false
                break
            }

            if (!targetSegments.any { targetSegment ->
                    /* Perform item validation. */
                    if (item.mediaItemId != targetSegment.first.mediaItemId) {
                        return@any false
                    }

                    /* Perform temporal validation. */
                    val outer = targetSegment.second.toMilliseconds()
                    !(outer.first > start || outer.second < end)
                }) {
                correct = false
                break
            }
        }


        answerSet.status = if (correct) {
            DbVerdictStatus.CORRECT
        } else {
            DbVerdictStatus.WRONG
        }
    }
}
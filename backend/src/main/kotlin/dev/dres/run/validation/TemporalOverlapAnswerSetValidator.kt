package dev.dres.run.validation

import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.time.TemporalRange
import dev.dres.data.model.submissions.*
import dev.dres.run.validation.interfaces.AnswerSetValidator
import kotlinx.dnq.query.iterator

/** */
typealias TransientMediaSegment = Pair<MediaItem, TemporalRange>

/**
 * A [AnswerSetValidator] class that checks, if a submission is correct based on the target segment and the
 * temporal overlap of the [DbSubmission] with the provided [TransientMediaSegment].
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
class TemporalOverlapAnswerSetValidator(private val targetSegments: Collection<TransientMediaSegment>) : AnswerSetValidator {

    override val deferring: Boolean = false

    /**
     * Validates the [DbAnswerSet] and updates its [DbVerdictStatus].
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
            val start = answer.start
            val end = answer.end
            val item = answer.item
            if (answer.type != DbAnswerType.TEMPORAL || item == null || start == null || end == null || start > end) {
                return
            }

            if (targetSegments.any { targetSegment ->
                    /* Perform item validation. */
                    if (item.id != targetSegment.first.mediaItemId) {
                        return@any false
                    }

                    /* Perform temporal validation. */
                    val outer = targetSegment.second.toMilliseconds()

                    return@any (outer.first <= start && outer.second >= start) || (outer.first <= end && outer.second >= end)
                }) {
                return
            }

        }

        /* If code reaches this point, the [DbAnswerSet] is correct. */
        answerSet.status = DbVerdictStatus.CORRECT
    }
}
package dev.dres.run.transformer

import dev.dres.api.rest.types.evaluation.submission.ApiClientAnswer
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.DbMediaSegment
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.submissions.DbAnswer
import dev.dres.data.model.submissions.DbAnswerType
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.run.transformer.basics.SubmissionTransformer
import kotlinx.dnq.query.FilteringContext.eq
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.isEmpty
import kotlinx.dnq.query.iterator
import kotlinx.dnq.query.singleOrNull

/**
 * A [SubmissionTransformer] that maps temporal [ApiClientSubmission]s such that temporal bounds align with pre-defined segments. Requires a transaction.
 *
 * @author Luca Rossetto
 */
class MapToSegmentTransformer : SubmissionTransformer {


    override fun transform(submission: ApiClientSubmission): ApiClientSubmission {

        return submission.copy(
            submission.answerSets.map { answerSet ->
                answerSet.copy(
                    answers = answerSet.answers.map { answer ->
                        if (answer.start != null || answer.end != null) {
                            transformAnswer(answer)
                        } else {
                            answer
                        }
                    }
                )
            }
        )

    }

    /**
     * Apples transformation to an individual [DbAnswer].
     *
     * @param answer The [DbAnswer] to transform.
     */
    private fun transformAnswer(answer: ApiClientAnswer): ApiClientAnswer {
        /* Extract item and find start and end segment. */
        /* Without a media item, there is nothing to map; the submission filters / validators take care of such answers. */
        val dbItem = answer.getDbItem() ?: return answer

        /* If the media item has no segments at all (e.g., images or videos without imported segments), the answer is left unchanged. */
        if (dbItem.segments.isEmpty) return answer

        val startSegment = answer.start?.let { DbMediaSegment.findContaining(dbItem, TemporalPoint.Millisecond(it)) }
        val endSegment = answer.end?.let { DbMediaSegment.findContaining(dbItem, TemporalPoint.Millisecond(it), isEnd = true) }

        /* Calculate bounds. */
        val bounds = when {
            startSegment != null && endSegment == null -> startSegment
            startSegment == null && endSegment != null -> endSegment
            startSegment == null && endSegment == null -> throw IllegalStateException("Cannot map answer time to segment, no matching segment found")
            startSegment == endSegment -> startSegment!!
            else -> throw IllegalStateException("Cannot map answer time to segment, range does not fall within one segment")
        }.range.toMilliseconds()

        return answer.copy(
            start = bounds.first,
            end = bounds.second
        )

    }
}
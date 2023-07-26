package dev.dres.run.transformer

import dev.dres.data.model.media.DbMediaSegment
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.submissions.DbAnswer
import dev.dres.data.model.submissions.DbAnswerType
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.run.transformer.basics.SubmissionTransformer
import kotlinx.dnq.query.iterator

/**
 * A [SubmissionTransformer] that maps temporal [DbSubmission]
 *
 * @author Luca Rossetto
 */
class MapToSegmentTransformer : SubmissionTransformer {

    /**
     * Apply this [MapToSegmentTransformer] to the provided [DbSubmission]. Transformation happens in place.
     *
     * Requires an ongoing transaction.
     *
     * @param submission [DbSubmission] to transform.
     */
    override fun transform(submission: DbSubmission)  {
        for (answerSet in submission.answerSets) {
            for (answer in answerSet.answers) {
                if (answer.type == DbAnswerType.TEMPORAL) {
                    transformAnswer(answer)
                }
            }
        }
    }

    /**
     * Apples transformation to an individual [DbAnswer].
     *
     * @param answer The [DbAnswer] to transform.
     */
    private fun transformAnswer(answer: DbAnswer) {
        /* Extract item and find start and end segment. */
        val item = answer.item ?: throw IllegalStateException("Media item not specified for answer.")
        val startSegment = answer.start?.let { DbMediaSegment.findContaining(item, TemporalPoint.Millisecond(it)) }
        val endSegment = answer.end?.let { DbMediaSegment.findContaining(item, TemporalPoint.Millisecond(it)) }

        /* Calculate bounds. */
        val bounds = when{
            startSegment != null && endSegment == null -> startSegment
            startSegment == null && endSegment != null -> endSegment
            startSegment == null && endSegment == null -> throw IllegalStateException("Cannot map answer time to segment, no matching segment found")
            startSegment == endSegment -> startSegment!!
            else -> throw IllegalStateException("Cannot map answer time to segment, range does not fall within one segment")
        }.range.toMilliseconds()

        /* Adjust start and end timestamp. */
        answer.start = bounds.first
        answer.end = bounds.second
    }
}
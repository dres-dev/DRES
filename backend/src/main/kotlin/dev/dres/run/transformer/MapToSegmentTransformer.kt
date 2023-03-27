package dev.dres.run.transformer

import dev.dres.api.rest.types.evaluation.ApiAnswer
import dev.dres.api.rest.types.evaluation.ApiAnswerType
import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.DbMediaSegment
import dev.dres.data.model.media.time.TemporalPoint
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query

class MapToSegmentTransformer : SubmissionTransformer {
    override fun transform(submission: ApiSubmission): ApiSubmission = submission.copy(
        answers = submission.answers.map { apiAnswerSet ->
            apiAnswerSet.copy(
                answers = apiAnswerSet.answers.map { mapAnswer(it) }
            )
        }
    )

    private fun mapAnswer(answer: ApiAnswer) : ApiAnswer {

        if (answer.type != ApiAnswerType.TEMPORAL) {
            return answer
        }

        val item = DbMediaItem.query(DbMediaItem::id eq answer.item?.mediaItemId).firstOrNull() ?: throw IllegalStateException("MediaItem with id ${answer.item?.mediaItemId} not found")


        val startSegment = answer.start?.let { DbMediaSegment.findContaining(item, TemporalPoint.Millisecond(it)) }
        val endSegment = answer.end?.let { DbMediaSegment.findContaining(item, TemporalPoint.Millisecond(it)) }

        val bounds = when{
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
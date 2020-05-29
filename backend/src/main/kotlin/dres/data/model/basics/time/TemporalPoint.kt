package dres.data.model.basics.time

/**
 * Notion of a [TemporalPoint] within a [MediaItem] that exhibits temporal development (e.g. [VideoItem]).
 *
 * @author Ralph Gasser
 * @version 1.0
 *
 * @param value Value of the [TemporalPoint]
 * @param value Unit of the [TemporalPoint]
 */
data class TemporalPoint(val value: Double, val unit: TemporalUnit)
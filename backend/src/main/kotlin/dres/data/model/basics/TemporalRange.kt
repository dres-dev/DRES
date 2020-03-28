package dres.data.model.basics

import kotlinx.serialization.Serializable

/**
 * Notion of a [TemporalRange] within a [MediaItem] that exhibits temporal development (e.g. [VideoItem].
 *
 * @author Ralph Gasser
 * @version 1.0
 *
 * @param start The start of the [TemporalRange]
 * @param end The end of the [TemporalRange]
 */
@Serializable
data class TemporalRange(val start: TemporalPoint, val end: TemporalPoint)
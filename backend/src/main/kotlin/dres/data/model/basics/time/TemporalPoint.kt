package dres.data.model.basics.time

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Notion of a [TemporalPoint] within a [MediaItem] that exhibits temporal development (e.g. [VideoItem]).
 *
 * @author Ralph Gasser
 * @version 1.0
 *
 * @param value Value of the [TemporalPoint]
 * @param value Unit of the [TemporalPoint]
 */
data class TemporalPoint @JsonCreator constructor(
        @JsonProperty("value") val value: Double,
        @JsonProperty("unit") val unit: TemporalUnit
)

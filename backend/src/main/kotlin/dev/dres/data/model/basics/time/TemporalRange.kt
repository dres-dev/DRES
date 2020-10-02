package dev.dres.data.model.basics.time

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import dev.dres.utilities.TimeUtil

/**
 * Notion of a [TemporalRange] within a [MediaItem] that exhibits temporal development (e.g. [VideoItem].
 *
 * @author Ralph Gasser
 * @version 1.1
 *
 * @param start The start of the [TemporalRange]
 * @param end The end of the [TemporalRange]
 */
data class TemporalRange constructor(val start: TemporalPoint, val end: TemporalPoint) {

    constructor(startMs: Long, endMs: Long): this(TemporalPoint(startMs.toDouble(), TemporalUnit.MILLISECONDS), TemporalPoint(endMs.toDouble(), TemporalUnit.MILLISECONDS))

    init {
        require(TimeUtil.toMilliseconds(start) <= TimeUtil.toMilliseconds(end)) {"Start point must be before End point in TemporalRange"}
    }

    fun contains(inner: TemporalRange, outerFps: Float = 24.0f, innerFps: Float = 24.0f): Boolean =
            TimeUtil.toMilliseconds(start, outerFps) <= TimeUtil.toMilliseconds(inner.start, innerFps) &&
                    TimeUtil.toMilliseconds(end, outerFps) >= TimeUtil.toMilliseconds(inner.end, innerFps)


    val center
        @JsonIgnore
        get() = (TimeUtil.toMilliseconds(end) - TimeUtil.toMilliseconds(start)) / 2

}

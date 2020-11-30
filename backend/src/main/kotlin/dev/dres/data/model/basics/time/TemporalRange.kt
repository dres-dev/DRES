package dev.dres.data.model.basics.time

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.utilities.TimeUtil
import kotlin.math.max
import kotlin.math.min

/**
 * Notion of a [TemporalRange] within a [MediaItem] that exhibits temporal development (e.g. [VideoItem].
 *
 * @author Ralph Gasser
 * @version 1.1.1
 *
 * @param start The start of the [TemporalRange]
 * @param end The end of the [TemporalRange]
 */
data class TemporalRange constructor(val start: TemporalPoint, val end: TemporalPoint) {

    constructor(startMs: Long, endMs: Long) : this(TemporalPoint(startMs.toDouble(), TemporalUnit.MILLISECONDS), TemporalPoint(endMs.toDouble(), TemporalUnit.MILLISECONDS))
    constructor(pair: Pair<Long, Long>) : this(pair.first, pair.second)

    init {
        require(TimeUtil.toMilliseconds(start) <= TimeUtil.toMilliseconds(end)) {"Start point must be before End point in TemporalRange"}
    }

    /**
     * Returns the duration of this [TemporalRange] in milliseconds.
     *
     * @param fps The fps based used for converting [TemporalUnit.FRAME_NUMBER]
     * @return The duration of this [TemporalRange] in milliseconds.
     */
    fun durationMs(fps: Float): Long = TimeUtil.toMilliseconds(this.end, fps) - TimeUtil.toMilliseconds(this.start, fps)

    fun contains(inner: TemporalRange, outerFps: Float = 24.0f, innerFps: Float = 24.0f): Boolean =
            TimeUtil.toMilliseconds(start, outerFps) <= TimeUtil.toMilliseconds(inner.start, innerFps) &&
                    TimeUtil.toMilliseconds(end, outerFps) >= TimeUtil.toMilliseconds(inner.end, innerFps)

    fun overlaps(other: TemporalRange, fps: Float = 24.0f, otherFps: Float = 24.0f) : Boolean {
        val s1 = TimeUtil.toMilliseconds(start, fps)
        val s2 = TimeUtil.toMilliseconds(other.start, fps)
        val e1 = TimeUtil.toMilliseconds(end, otherFps)
        val e2 = TimeUtil.toMilliseconds(other.end, otherFps)

        return  min(e1, e2) < max(s1, s2)
    }

    val center
        @JsonIgnore
        get() = (TimeUtil.toMilliseconds(end) - TimeUtil.toMilliseconds(start)) / 2

}

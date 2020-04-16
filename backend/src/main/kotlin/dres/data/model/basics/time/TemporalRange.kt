package dres.data.model.basics.time

import dres.utilities.TimeUtil
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
data class TemporalRange(val start: TemporalPoint, val end: TemporalPoint) {

    constructor(startMs: Long, endMs: Long): this(TemporalPoint(startMs.toDouble(), TemporalUnit.MILLISECONDS), TemporalPoint(endMs.toDouble(), TemporalUnit.MILLISECONDS))

    init {
        require(TimeUtil.toMilliseconds(start) <= TimeUtil.toMilliseconds(end)) {"Start point must be before End point in TemporalRange"}
    }

    fun contains(inner: TemporalRange, outerFps: Float = 24.0f, innerFps: Float = 24.0f): Boolean =
            TimeUtil.toMilliseconds(start, outerFps) <= TimeUtil.toMilliseconds(inner.start, innerFps) &&
                    TimeUtil.toMilliseconds(end, outerFps) >= TimeUtil.toMilliseconds(inner.end, innerFps)

}

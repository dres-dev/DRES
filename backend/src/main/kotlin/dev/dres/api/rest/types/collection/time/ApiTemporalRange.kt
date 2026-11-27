package dev.dres.api.rest.types.collection.time

import dev.dres.data.model.media.time.TemporalRange
import kotlinx.serialization.Serializable

/**
 * RESTful API representation of a [TemporalRange].
 *
 * @version 1.0.0
 * @author Ralph Gasser
 */
@Serializable
data class ApiTemporalRange(val start: ApiTemporalPoint, val end: ApiTemporalPoint) {
    constructor(range: TemporalRange) : this(ApiTemporalPoint.fromTemporalPoint(range.start), ApiTemporalPoint.fromTemporalPoint(range.end))
    fun toTemporalRange(fps: Float): TemporalRange = TemporalRange(start.toTemporalPoint(fps), end.toTemporalPoint(fps))

    /**
     * The bounds of this range in milliseconds.
     *
     * A range that travels through the entity store is already expressed in milliseconds, so no frame rate
     * is needed; [fps] only matters for a range given in frames or as a timecode, which is why it is
     * optional here instead of forced with `!!` at the call site.
     *
     * @param fps Frame rate, required only when a bound is expressed in frames or as a timecode.
     * @return The start and end in milliseconds.
     */
    fun toMilliseconds(fps: Float = 0.0f): Pair<Long, Long> {
        val range = this.toTemporalRange(fps)
        return range.start.toMilliseconds() to range.end.toMilliseconds()
    }
}
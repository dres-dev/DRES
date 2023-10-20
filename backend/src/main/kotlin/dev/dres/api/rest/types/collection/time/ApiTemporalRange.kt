package dev.dres.api.rest.types.collection.time

import dev.dres.data.model.media.time.TemporalRange

/**
 * RESTful API representation of a [TemporalRange].
 *
 * @version 1.0.0
 * @author Ralph Gasser
 */
data class ApiTemporalRange(val start: ApiTemporalPoint, val end: ApiTemporalPoint) {
    constructor(range: TemporalRange) : this(ApiTemporalPoint.fromTemporalPoint(range.start), ApiTemporalPoint.fromTemporalPoint(range.end))
    fun toTemporalRange(fps: Float): TemporalRange = TemporalRange(start.toTemporalPoint(fps), end.toTemporalPoint(fps))
}
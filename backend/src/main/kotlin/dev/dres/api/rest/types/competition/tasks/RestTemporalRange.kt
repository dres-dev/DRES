package dev.dres.api.rest.types.competition.tasks

import dev.dres.data.model.basics.time.*

//TODO this could be made a bit nicer but that will require api changes

data class RestTemporalRange(val start: RestTemporalPoint, val end: RestTemporalPoint) {
    constructor(range: TemporalRange) : this(RestTemporalPoint.fromTemporalPoint(range.start), RestTemporalPoint.fromTemporalPoint(range.end))
    fun toTemporalRange(fps: Float): TemporalRange = TemporalRange(start.toTemporalPoint(fps), end.toTemporalPoint(fps))
}

data class RestTemporalPoint(val value: Double, val unit: RestTemporalUnit) {

    companion object{
        fun fromTemporalPoint(temporalPoint: TemporalPoint): RestTemporalPoint = when(temporalPoint){
            is TemporalPoint.Frame -> RestTemporalPoint(temporalPoint.frame.toDouble(), RestTemporalUnit.FRAME_NUMBER)
            is TemporalPoint.Millisecond -> RestTemporalPoint(temporalPoint.millisecond.toDouble(), RestTemporalUnit.MILLISECONDS)
            is TemporalPoint.Timecode -> RestTemporalPoint(temporalPoint.toMilliseconds().toDouble(), RestTemporalUnit.MILLISECONDS)
        }
    }

    fun toTemporalPoint(fps: Float): TemporalPoint = when(this.unit){
        RestTemporalUnit.FRAME_NUMBER -> TemporalPoint.Frame(value.toInt(), fps)
        RestTemporalUnit.SECONDS -> TemporalPoint.Millisecond((value * 1000).toLong())
        RestTemporalUnit.MILLISECONDS -> TemporalPoint.Millisecond(value .toLong())
    }
}

enum class RestTemporalUnit {
    FRAME_NUMBER,
    SECONDS,
    MILLISECONDS
}
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
            is FrameTemporalPoint -> RestTemporalPoint(temporalPoint.frame.toDouble(), RestTemporalUnit.FRAME_NUMBER)
            is MilliSecondTemporalPoint -> RestTemporalPoint(temporalPoint.millisecond.toDouble(), RestTemporalUnit.MILLISECONDS)
            is TimeCodeTemporalPoint -> RestTemporalPoint(temporalPoint.toMilliseconds().toDouble(), RestTemporalUnit.MILLISECONDS)
        }
    }

    fun toTemporalPoint(fps: Float): TemporalPoint = when(this.unit){
        RestTemporalUnit.FRAME_NUMBER -> FrameTemporalPoint(value.toInt(), fps)
        RestTemporalUnit.SECONDS -> MilliSecondTemporalPoint((value * 1000).toLong())
        RestTemporalUnit.MILLISECONDS -> MilliSecondTemporalPoint(value .toLong())
    }
}

enum class RestTemporalUnit {
    FRAME_NUMBER,
    SECONDS,
    MILLISECONDS
}
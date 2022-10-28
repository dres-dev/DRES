package dev.dres.api.rest.types.competition.tasks

import dev.dres.data.model.basics.time.*
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange

data class RestTemporalRange(val start: RestTemporalPoint, val end: RestTemporalPoint) {
    constructor(range: TemporalRange) : this(RestTemporalPoint.fromTemporalPoint(range.start), RestTemporalPoint.fromTemporalPoint(range.end))
    fun toTemporalRange(fps: Float): TemporalRange = TemporalRange(start.toTemporalPoint(fps), end.toTemporalPoint(fps))
}

data class RestTemporalPoint(val value: String, val unit: RestTemporalUnit) {

    companion object{
        fun fromTemporalPoint(temporalPoint: TemporalPoint): RestTemporalPoint = when(temporalPoint){
            is TemporalPoint.Frame -> RestTemporalPoint(temporalPoint.frame.toString(), RestTemporalUnit.FRAME_NUMBER)
            is TemporalPoint.Millisecond -> RestTemporalPoint(temporalPoint.millisecond.toString(), RestTemporalUnit.MILLISECONDS)
            is TemporalPoint.Timecode -> RestTemporalPoint(temporalPoint.toMilliseconds().toString(), RestTemporalUnit.MILLISECONDS)
        }
    }

    fun toTemporalPoint(fps: Float): TemporalPoint = when(this.unit){
        RestTemporalUnit.FRAME_NUMBER -> TemporalPoint.Frame(value.toDouble().toInt(), fps)
        RestTemporalUnit.SECONDS -> TemporalPoint.Millisecond((value.toDouble() * 1000).toLong())
        RestTemporalUnit.MILLISECONDS -> TemporalPoint.Millisecond(value.toDouble().toLong())
        RestTemporalUnit.TIMECODE -> TemporalPoint.Timecode(value, fps)
    }
}

enum class RestTemporalUnit {
    FRAME_NUMBER,
    SECONDS,
    MILLISECONDS,
    TIMECODE
}
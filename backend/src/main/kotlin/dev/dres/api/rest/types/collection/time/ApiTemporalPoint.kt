package dev.dres.api.rest.types.collection.time

import dev.dres.data.model.media.time.TemporalPoint
import kotlinx.serialization.Serializable

/**
 * RESTful API representation of a [TemporalPoint].
 *
 * @version 1.0.0
 * @author Ralph Gasser
 */
@Serializable
data class ApiTemporalPoint(val value: String, val unit: ApiTemporalUnit) {

    companion object{
        fun fromTemporalPoint(temporalPoint: TemporalPoint): ApiTemporalPoint = when(temporalPoint){
            is TemporalPoint.Frame -> ApiTemporalPoint(temporalPoint.frame.toString(), ApiTemporalUnit.FRAME_NUMBER)
            is TemporalPoint.Millisecond -> ApiTemporalPoint(temporalPoint.millisecond.toString(), ApiTemporalUnit.MILLISECONDS)
            is TemporalPoint.Timecode -> ApiTemporalPoint(temporalPoint.toMilliseconds().toString(), ApiTemporalUnit.MILLISECONDS)
        }
    }

    fun toTemporalPoint(fps: Float): TemporalPoint = when(this.unit){
        ApiTemporalUnit.FRAME_NUMBER -> TemporalPoint.Frame(value.toDouble().toInt(), fps)
        ApiTemporalUnit.SECONDS -> TemporalPoint.Millisecond((value.toDouble() * 1000).toLong())
        ApiTemporalUnit.MILLISECONDS -> TemporalPoint.Millisecond(value.toDouble().toLong())
        ApiTemporalUnit.TIMECODE -> TemporalPoint.Timecode(value, fps)
    }
}
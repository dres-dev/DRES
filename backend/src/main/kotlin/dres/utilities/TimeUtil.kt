package dres.utilities

import dres.data.model.basics.TemporalPoint
import dres.data.model.basics.TemporalRange
import dres.data.model.basics.TemporalUnit

object TimeUtil {

    fun toMilliseconds(point: TemporalPoint, fps: Float = 24.0f): Long {
        return when (point.unit) {
            TemporalUnit.FRAME_NUMBER -> (point.value * fps * 1000).toLong()
            TemporalUnit.SECONDS -> (point.value * 1000).toLong()
            TemporalUnit.MILLISECONDS -> point.value.toLong()
        }
    }

    fun toMilliseconds(range: TemporalRange, fps: Float = 24.0f): Pair<Long, Long> = (
            toMilliseconds(range.start, fps) to toMilliseconds(range.end, fps)
            )

}
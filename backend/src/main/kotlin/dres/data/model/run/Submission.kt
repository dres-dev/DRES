package dres.data.model.run

import com.fasterxml.jackson.annotation.JsonIgnore
import dres.data.model.basics.media.MediaItem
import dres.data.model.basics.time.TemporalPoint
import dres.data.model.basics.time.TemporalRange
import dres.data.model.basics.time.TemporalUnit
import kotlinx.serialization.Serializable

/**
 * A [Submission] as received by a competition participant.
 *
 * @author Ralph Gasser & Luca Rossetto
 * @version 1.0
 */

@Serializable
data class Submission(val team: Int,
                      val member: Long,
                      val timestamp: Long,
                      val item: MediaItem,
                      val start: Long? = null, //in ms
                      val end: Long? = null //in ms
) {

    var status: SubmissionStatus = SubmissionStatus.INDETERMINATE

    @Transient
    @JsonIgnore
    var taskRun: TaskRunData? = null
    internal set

    fun temporalRange(): TemporalRange {
        if (start == null && end == null) {
            val zero = TemporalPoint(0.0, TemporalUnit.MILLISECONDS)
            return TemporalRange(zero, zero)
        }
        if (start != null && end != null){
            return TemporalRange(TemporalPoint(start.toDouble(), TemporalUnit.MILLISECONDS), TemporalPoint(end.toDouble(), TemporalUnit.MILLISECONDS))
        }
        val point = TemporalPoint(start?.toDouble()
                ?: end!!.toDouble(), TemporalUnit.MILLISECONDS)
        return TemporalRange(point, point)
    }
}
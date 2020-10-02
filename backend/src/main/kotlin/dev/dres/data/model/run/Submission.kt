package dev.dres.data.model.run

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.basics.time.TemporalPoint
import dev.dres.data.model.basics.time.TemporalRange
import dev.dres.data.model.basics.time.TemporalUnit
import java.util.*

/**
 * A [Submission] as received by a competition participant.
 *
 * @author Ralph Gasser & Luca Rossetto
 * @version 1.0
 */

data class Submission(val team: Int,
                      val member: UID,
                      val timestamp: Long,
                      val item: MediaItem,
                      val start: Long? = null, //in ms
                      val end: Long? = null, //in ms
                      val uid: String = UUID.randomUUID().toString()
) {

    var status: SubmissionStatus = SubmissionStatus.INDETERMINATE

    @Transient
    @JsonIgnore
    var taskRun: CompetitionRun.TaskRun? = null
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
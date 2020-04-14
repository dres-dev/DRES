package dres.data.model.run

import dres.data.model.basics.TemporalPoint
import dres.data.model.basics.TemporalRange
import dres.data.model.basics.TemporalUnit
import kotlinx.serialization.Serializable

/**
 * A [Submission] as received by a competition participant.
 *
 * @author Ralph Gasser & Luca Rossetto
 * @version 1.0
 */

@Serializable
data class Submission(val team: Int, val tool: Int, val timestamp: Long, val collection: String, val item: String,
                      val start: Long? = null, //in ms
                      val end: Long? = null //in ms
) {
    var status: SubmissionStatus = SubmissionStatus.INDETERMINATE

    //@Transient
    //var taskRun: CompetitionRun.TaskRun? = null
    //internal set

    fun temporalRange(): TemporalRange {
        if (start == null && end == null) {
            val zero = TemporalPoint(0.0, TemporalUnit.MILLISECONDS)
            return TemporalRange(zero, zero)
        }
        if (start != null && end != null){
            return TemporalRange(TemporalPoint(start.toDouble(), TemporalUnit.MILLISECONDS), TemporalPoint(end.toDouble(), TemporalUnit.MILLISECONDS))
        }
        val point = TemporalPoint(start?.toDouble() ?: end!!.toDouble(), TemporalUnit.MILLISECONDS)
        return TemporalRange(point, point)
    }
}
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
 * @version 1.0.1
 */

interface BaseSubmissionAspect {
    val uid: UID
    val teamId: UID
    val memberId: UID
    val timestamp: Long
    val item: MediaItem
    var status: SubmissionStatus
}

interface TemporalSubmissionAspect : BaseSubmissionAspect {

    /** Start time in milliseconds */
    val start: Long

    /** End time in milliseconds */
    val end: Long

    val temporalRange: TemporalRange
}

interface SpatialSubmissionAspect : BaseSubmissionAspect {
    //TODO some spatial representation
}

sealed class Submission(override val teamId: UID,
                        override val memberId: UID,
                        override val timestamp: Long,
                        override val item: MediaItem,
                        override val uid: UID
) : BaseSubmissionAspect {

    override var status: SubmissionStatus = SubmissionStatus.INDETERMINATE

    @Transient
    @JsonIgnore
    var taskRun: CompetitionRun.TaskRun? = null
        internal set


}

data class ItemSubmission(override val teamId: UID,
                          override val memberId: UID,
                          override val timestamp: Long,
                          override val item: MediaItem,
                          override val uid: UID = UID()
) : Submission(teamId, memberId, timestamp, item, uid)

data class TemporalSubmission(override val teamId: UID,
                              override val memberId: UID,
                              override val timestamp: Long,
                              override val item: MediaItem,
                              override val start: Long, //in ms
                              override val end: Long, //in ms
                              override val uid: UID = UID()
) : Submission(teamId, memberId, timestamp, item, uid), TemporalSubmissionAspect {

    override val temporalRange: TemporalRange
        get() = TemporalRange(TemporalPoint(start.toDouble(), TemporalUnit.MILLISECONDS), TemporalPoint(end.toDouble(), TemporalUnit.MILLISECONDS))

}
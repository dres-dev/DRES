package dev.dres.data.model.run

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.basics.time.TemporalPoint
import dev.dres.data.model.basics.time.TemporalRange
import dev.dres.data.model.basics.time.TemporalUnit

/**
 * A [Submission] as received by a competition participant.
 *
 * @author Ralph Gasser & Luca Rossetto
 * @version 1.0.1
 */

interface StatusAspect {
    var status: SubmissionStatus
}

interface ItemAspect {
    val item: MediaItem
}

interface OriginAspect {
    val uid: UID
    val teamId: UID
    val memberId: UID
}

interface BaseSubmissionAspect : StatusAspect, ItemAspect, OriginAspect {
    val timestamp: Long
    fun task(): Task?
}

interface TemporalAspect {

    /** Start time in milliseconds */
    val start: Long

    /** End time in milliseconds */
    val end: Long

    val temporalRange: TemporalRange
}

interface TemporalSubmissionAspect : BaseSubmissionAspect, TemporalAspect

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
    internal var task: InteractiveCompetitionRun.TaskRun? = null

    override fun task(): InteractiveCompetitionRun.TaskRun? = task


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

interface ResultBatch<T> where T : ItemAspect, T : StatusAspect {
    val task: TaskId
    val name: String
    val results: List<T>
}

data class TemporalBatchElement(
    override val item: MediaItem,
    override val start: Long,
    override val end: Long,
    override val temporalRange: TemporalRange,
) : ItemAspect, TemporalAspect, StatusAspect {
    override var status: SubmissionStatus = SubmissionStatus.INDETERMINATE
}

interface BaseSubmissionBatch<R: ResultBatch<*>> : OriginAspect {
    val results : Collection<R>
}

data class TemporalSubmissionBatch
    (
    override val teamId: UID,
    override val memberId: UID,
    override val uid: UID,
    override val results: Collection<ResultBatch<TemporalBatchElement>>,
) : BaseSubmissionBatch<ResultBatch<TemporalBatchElement>>
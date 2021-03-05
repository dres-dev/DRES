package dev.dres.data.model.submissions

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.basics.time.TemporalPoint
import dev.dres.data.model.basics.time.TemporalRange
import dev.dres.data.model.basics.time.TemporalUnit
import dev.dres.data.model.run.AbstractInteractiveTask
import dev.dres.data.model.run.interfaces.Task
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.aspects.BaseSubmissionAspect
import dev.dres.data.model.submissions.aspects.TemporalSubmissionAspect


/**
 * A [Submission] as received by a competition participant.
 *
 * @author Ralph Gasser & Luca Rossetto
 * @version 1.1.0
 */
sealed class Submission : BaseSubmissionAspect {

    /** The [AbstractInteractiveTask] this [Submission] belongs to. */
    override var task: AbstractInteractiveTask? = null

    /** The [SubmissionStatus] of this [Submission]. */
    override var status: SubmissionStatus = SubmissionStatus.INDETERMINATE

    /**
     *
     */
    data class Item(
        override val teamId: UID,
        override val memberId: UID,
        override val timestamp: Long,
        override val item: MediaItem,
        override val uid: UID = UID()
    ) : Submission()

    /**
     *
     */
    data class Temporal(
        override val teamId: UID,
        override val memberId: UID,
        override val timestamp: Long,
        override val item: MediaItem,
        override val start: Long, //in ms
        override val end: Long, //in ms
        override val uid: UID = UID()
    ) : Submission(), TemporalSubmissionAspect {

        override val temporalRange: TemporalRange
            get() = TemporalRange(TemporalPoint(start.toDouble(), TemporalUnit.MILLISECONDS), TemporalPoint(end.toDouble(), TemporalUnit.MILLISECONDS))
    }
}
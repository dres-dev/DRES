package dev.dres.data.model.submissions

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.admin.User
import dev.dres.data.model.competition.team.Team
import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange
import dev.dres.data.model.run.Task
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.simple.min
import kotlinx.dnq.simple.requireIf

typealias SubmissionId = String

/**
 * A [Submission] as received by a competition participant.
 *
 * @author Ralph Gasser & Luca Rossetto
 * @version 1.1.0
 */
sealed class Submission(entity: Entity) : PersistentEntity(entity) {
    companion object : XdNaturalEntityType<Submission>()

    /** The [SubmissionId] of this [User]. */
    var submissionId: SubmissionId
        get() = this.id
        set(value) { this.id = value }

    /** The timestamp of this [Submission]. */
    var timestamp by xdRequiredLongProp { min(0L) }

    /** The [SubmissionStatus] of this [Submission]. */
    var status by xdLink1(SubmissionStatus)

    /** The [Task] this [Submission] belongs to. */
    var task by xdParent<Submission,Task>(Task::submissions)

    /** The [Team] that submitted this [Submission] */
    var team by xdLink1(Team)

    /** The [User] that submitted this [Submission] */
    var user by xdLink1(User)

    /** The [SubmissionType] of this [Submission]. */
    var type by xdLink1(SubmissionType)

    /** The [MediaItem] submitted. Only for [SubmissionType.ITEM] or [SubmissionType.TEMPORAL]. */
    var item by xdLink0_1(MediaItem)

    /** The start frame number of this [Submission]. */
    var start by xdRequiredLongProp { requireIf { this.type == SubmissionType.TEMPORAL } }

    /** The end frame number of this [Submission]. */
    var end by xdRequiredLongProp { requireIf { this.type == SubmissionType.TEMPORAL } }

    /** The text submitted. Only for [SubmissionType.TEXT] . */
    var text by xdStringProp { requireIf { this.type == SubmissionType.TEXT } }

    /**  */
    val temporalRange: TemporalRange
        get() = TemporalRange(TemporalPoint.Millisecond(start), TemporalPoint.Millisecond(end))
}
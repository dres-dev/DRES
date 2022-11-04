package dev.dres.data.model.submissions

import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.admin.User
import dev.dres.data.model.template.team.Team
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
 * A [Submission] as submitted by a competition participant and received by DRES.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @version 2.0.0
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
    var start by xdNullableLongProp { requireIf { this.type == SubmissionType.TEMPORAL } }

    /** The end frame number of this [Submission]. */
    var end by xdNullableLongProp { requireIf { this.type == SubmissionType.TEMPORAL } }

    /** The text submitted. Only for [SubmissionType.TEXT] . */
    var text by xdStringProp { requireIf { this.type == SubmissionType.TEXT } }

    /**  Returns the [TemporalRange] for this [Submission]. */
    val temporalRange: TemporalRange?
        get() = try {
            TemporalRange(TemporalPoint.Millisecond(this.start!!), TemporalPoint.Millisecond(this.end!!))
        } catch (e: NullPointerException) {
            null
        }

    /**
     * Converts this [Submission] to a RESTful API representation [ApiSubmission].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiSubmission]
     */
    fun toApi(blind: Boolean = false): ApiSubmission = ApiSubmission(
        id = this.id,
        teamId = this.team.id,
        teamName = this.team.name,
        memberId = this.user.id,
        memberName = this.user.username,
        status = this.status,
        timestamp = this.timestamp,
        item = if (!blind) { this.item?.toApi() } else { null },
        text =  if (!blind) { this.text } else { null },
        start = if (!blind) { this.start } else { null },
        end = if (!blind) { this.end } else { null }
    )

}
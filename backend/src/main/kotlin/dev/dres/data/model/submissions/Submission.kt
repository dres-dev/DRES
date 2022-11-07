package dev.dres.data.model.submissions

import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.admin.User
import dev.dres.data.model.template.team.Team
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.simple.min

typealias SubmissionId = String

/**
 * A [Submission] as submitted by a competition participant and received by DRES.
 *
 * Contains one to N [Verdict]s regarding a [Task].
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

    /** The [Team] that submitted this [Submission] */
    var team by xdLink1(Team)

    /** The [User] that submitted this [Submission] */
    var user by xdLink1(User)

    /** The [Verdict]s that make-up this [Submission]. For batched submissions, more than one verdict can be possible. */
    val verdicts by xdChildren1_N<Submission,Verdict>(Verdict::submission)

    /**
     * Converts this [Submission] to a RESTful API representation [ApiSubmission].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @param blind True, if a "blind" Submission (without [verdicts]) should be generated.
     * @return [ApiSubmission]
     */
    fun toApi(blind: Boolean = false): ApiSubmission = ApiSubmission(
        id = this.id,
        teamId = this.team.id,
        teamName = this.team.name,
        memberId = this.user.id,
        memberName = this.user.username,
        timestamp = this.timestamp,
        verdicts = if (blind) { emptyList() } else { this.verdicts.asSequence().map { it.toApi() }.toList()}
    )
}
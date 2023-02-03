package dev.dres.data.model.submissions

import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.template.team.DbTeam
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.simple.min

typealias SubmissionId = String

/**
 * A [DbSubmission] as submitted by a competition participant and received by DRES.
 *
 * Contains one to N [DbAnswerSet]s regarding a [Task].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @version 2.0.0
 */
class DbSubmission(entity: Entity) : PersistentEntity(entity) {
    companion object : XdNaturalEntityType<DbSubmission>()

    /** The [SubmissionId] of this [DbUser]. */
    var submissionId: SubmissionId
        get() = this.id
        set(value) { this.id = value }

    /** The timestamp of this [DbSubmission]. */
    var timestamp by xdRequiredLongProp { min(0L) }

    /** The [DbTeam] that submitted this [DbSubmission] */
    var team by xdLink1(DbTeam)

    /** The [DbUser] that submitted this [DbSubmission] */
    var user by xdLink1(DbUser)

    /** The [DbAnswerSet]s that make-up this [DbSubmission]. For batched submissions, more than one verdict can be possible. */
    val verdicts by xdChildren1_N<DbSubmission,DbAnswerSet>(DbAnswerSet::submission)

    /**
     * Converts this [DbSubmission] to a RESTful API representation [ApiSubmission].
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
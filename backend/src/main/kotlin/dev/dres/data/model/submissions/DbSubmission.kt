package dev.dres.data.model.submissions

import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.template.team.TeamId
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.first
import kotlinx.dnq.simple.min

/**
 * A [DbSubmission] as submitted by a competition participant and received by DRES.
 *
 * Contains one to N [DbAnswerSet]s regarding a [Task].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @version 2.0.0
 */
class DbSubmission(entity: Entity) : PersistentEntity(entity), Submission {
    companion object : XdNaturalEntityType<DbSubmission>()

    /** The [SubmissionId] of this [DbUser]. */
    override var submissionId: SubmissionId
        get() = this.id
        set(value) { this.id = value }

    /** The timestamp of this [DbSubmission]. */
    override var timestamp by xdRequiredLongProp { min(0L) }

    /** The [DbTeam] that submitted this [DbSubmission] */
    var team by xdLink1(DbTeam)
    override val teamId: TeamId
        get() = this.team.teamId

    /** The [DbUser] that submitted this [DbSubmission] */
    var user by xdLink1(DbUser)

    override val memberId: UserId
        get() = this.user.userId
    override val evaluationId: EvaluationId
        get() = this.answerSets.first().task.evaluation.evaluationId

    /** The [DbAnswerSet]s that make-up this [DbSubmission]. For batched submissions, more than one verdict can be possible. */
    val answerSets by xdChildren1_N<DbSubmission,DbAnswerSet>(DbAnswerSet::submission)

    override fun answerSets(): Sequence<AnswerSet> = answerSets.asSequence()

    /**
     * Converts this [DbSubmission] to a RESTful API representation [ApiSubmission].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @param blind True, if a "blind" Submission (without [answerSets]) should be generated.
     * @return [ApiSubmission]
     */
    fun toApi(blind: Boolean = false): ApiSubmission = ApiSubmission(
        submissionId = this.id,
        teamId = this.team.id,
        teamName = this.team.name,
        memberId = this.user.id,
        memberName = this.user.username,
        timestamp = this.timestamp,
        answers = this.answerSets.asSequence().map { it.toApi(blind) }.toList(),
        evaluationId = this.evaluationId
    )
}

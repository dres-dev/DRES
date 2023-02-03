package dev.dres.data.model.template.team

import dev.dres.api.rest.types.competition.team.ApiTeam
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.template.DbEvaluationTemplate
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy
import kotlinx.dnq.query.asSequence


/** The ID of a [DbTeam]. */
typealias TeamId = String

/**
 * Represents a [DbTeam] that takes part in a competition managed by DRES.
 *
 * @author Ralph Gasser, Loris Sauter, Luca Rossetto
 * @version 2.0.0
 */
class DbTeam(entity: Entity) : PersistentEntity(entity) {
    companion object: XdNaturalEntityType<DbTeam>() {
        /** Combination of [DbTeam] name / competition must be unique. */
        override val compositeIndices = listOf(
            listOf(DbTeam::name, DbTeam::evaluation)
        )
    }

    /** The [TeamId] of this [DbTeam]. */
    var teamId: TeamId
        get() = this.id
        set(value) { this.id = value }

    /** The name held by this [DbTeam]. Must be unique!*/
    var name by xdRequiredStringProp(unique = false, trimmed = true)

    /** The color used by this [DbTeam]. HTML colour code. */
    var color by xdRequiredStringProp(unique = false, trimmed = true)

    /** Logo used by this [DbTeam] as Blob. */
    var logo by xdBlobProp()

    /** The [DbEvaluationTemplate] this [DbTeam] belongs to. */
    val evaluation: DbEvaluationTemplate by xdParent(DbEvaluationTemplate::teams)

    /** The [DbTeamGroup] this [DbTeam] belongs to (or null if not assigned to a group). */
    var group: DbTeamGroup? by xdLink0_1(DbTeamGroup::teams)

    /** The [DbUser]s that belong to this [DbTeam]. */
    val users by xdLink0_N(DbUser, onDelete = OnDeletePolicy.CLEAR, onTargetDelete = OnDeletePolicy.CLEAR)

    /**
     * Converts this [DbTeam] to a RESTful API representation [ApiTeam].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiTeam]
     */
    fun toApi() = ApiTeam(this.teamId, this.name, this.color, this.users.asSequence().map { it.toApi() }.toList())
}
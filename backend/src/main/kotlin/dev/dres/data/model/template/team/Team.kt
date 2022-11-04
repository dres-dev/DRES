package dev.dres.data.model.template.team

import dev.dres.api.rest.types.competition.team.ApiTeam
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.admin.User
import dev.dres.data.model.template.EvaluationTemplate
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy
import kotlinx.dnq.query.asSequence


/** The ID of a [Team]. */
typealias TeamId = String

/**
 * Represents a [Team] that takes part in a competition managed by DRES.
 *
 * @author Ralph Gasser, Loris Sauter, Luca Rossetto
 * @version 2.0.0
 */
class Team(entity: Entity) : PersistentEntity(entity) {
    companion object: XdNaturalEntityType<Team>() {
        /** Combination of [Team] name / competition must be unique. */
        override val compositeIndices = listOf(
            listOf(Team::name, Team::competition)
        )
    }

    /** The [TeamId] of this [Team]. */
    var teamId: TeamId
        get() = this.id
        set(value) { this.id = value }

    /** The name held by this [Team]. Must be unique!*/
    var name by xdRequiredStringProp(unique = false, trimmed = true)

    /** The color used by this [Team]. HTML colour code. */
    var color by xdRequiredStringProp(unique = false, trimmed = true)

    /** Logo used by this [Team] as Blob. */
    var logo by xdBlobProp()

    /** The [EvaluationTemplate] this [Team] belongs to. */
    var competition by xdParent<Team,EvaluationTemplate>(EvaluationTemplate::teams)

    /** The [TeamGroup] this [Team] belongs to (or null if not assigned to a group). */
    var group by xdLink0_1(TeamGroup::teams)

    /** The [User]s that belong to this [Team]. */
    val users by xdLink0_N(User, onDelete = OnDeletePolicy.CLEAR, onTargetDelete = OnDeletePolicy.CLEAR)

    /**
     * Converts this [Team] to a RESTful API representation [ApiTeam].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiTeam]
     */
    fun toApi() = ApiTeam(this.teamId, this.name, this.color, this.users.asSequence().map { it.toApi() }.toList())
}
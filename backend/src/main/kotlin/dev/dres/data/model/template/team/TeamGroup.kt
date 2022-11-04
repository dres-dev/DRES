package dev.dres.data.model.template.team

import dev.dres.api.rest.types.competition.team.ApiTeamGroup
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.admin.User
import dev.dres.data.model.template.EvaluationTemplate
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.toList

typealias TeamGroupId = String

/**
 * Represents a [TeamGroup] that takes part in a competition managed by DRES.
 *
 * Can be used to aggregate score values across [Team]s
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
class TeamGroup(entity: Entity) : PersistentEntity(entity) {

    companion object: XdNaturalEntityType<TeamGroup>()

    /** The [UserId] of this [User]. */
    var teamGroupId: TeamGroupId
        get() = this.id
        set(value) { this.id = value }

    /** The name held by this [User]. Must be unique!*/
    var name by xdRequiredStringProp(unique = false, trimmed = false)

    /** The default [TeamAggregator] to use for this [TeamGroup]. */
    var defaultAggregator by xdLink1(TeamAggregator)

    /** The [EvaluationTemplate] this [Team] belongs to. */
    var competition by xdParent<TeamGroup,EvaluationTemplate>(EvaluationTemplate::teamsGroups)

    /** The [Team]s that belong to this [TeamGroup]. */
    val teams by xdLink0_N<TeamGroup,Team>(Team::group)


    /**
     * Converts this [TeamGroup] to a RESTful API representation [ApiTeamGroup].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiTeamGroup]
     */
    fun toApi() = ApiTeamGroup(this.teamGroupId, this.name, this.teams.asSequence().map { it.toApi() }.toList(), this.defaultAggregator.name)

    /**
     * Returns a new [TeamAggregatorImpl] for this [TeamGroup].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [TeamAggregatorImpl]
     */
    fun newAggregator() : TeamAggregatorImpl = this.defaultAggregator.newInstance(this.teams.toList())
}
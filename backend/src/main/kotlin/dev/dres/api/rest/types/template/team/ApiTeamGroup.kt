package dev.dres.api.rest.types.template.team

import dev.dres.data.model.template.team.DbTeamGroup
import dev.dres.data.model.template.team.TeamAggregatorImpl
import dev.dres.data.model.template.team.TeamGroupId

/**
 * A RESTful API representation of a [DbTeamGroup]
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
data class ApiTeamGroup(
    val id: TeamGroupId? = null,
    val name: String? = null,
    val teams: List<ApiTeam> = emptyList(),
    val aggregation: ApiTeamAggregatorType
) {
    /**
     * Returns a new [TeamAggregatorImpl] for this [DbTeamGroup].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [TeamAggregatorImpl]
     */
    fun newAggregator() : TeamAggregatorImpl = this.aggregation.newInstance(this.teams.map { it.teamId }.toSet())
}

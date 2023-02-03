package dev.dres.api.rest.types.competition.team

import dev.dres.data.model.template.team.DbTeamGroup
import dev.dres.data.model.template.team.TeamGroupId

/**
 * A RESTful API representation of a [DbTeamGroup]
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
data class ApiTeamGroup(val id: TeamGroupId, val name: String, val teams: List<ApiTeam>, val aggregation: String)
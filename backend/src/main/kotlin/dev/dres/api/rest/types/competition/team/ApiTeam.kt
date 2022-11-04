package dev.dres.api.rest.types.competition.team

import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.template.team.Team
import dev.dres.data.model.template.team.TeamId

/**
 * A RESTful API representation of a [Team]
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
data class ApiTeam(
    val teamId: TeamId,
    val name: String,
    val color: String,
    val users: List<ApiUser> = emptyList(),
    var logoData: String? = null
)
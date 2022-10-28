package dev.dres.api.rest.types.competition

import dev.dres.api.rest.types.users.ApiUser

/**
 * A RESTful API representation of a [Team]
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
data class ApiTeam(val name: String, val color: String, val logoId: String, val users: List<ApiUser> = emptyList())
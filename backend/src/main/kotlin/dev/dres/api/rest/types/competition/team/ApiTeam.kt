package dev.dres.api.rest.types.competition.team

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.template.team.Team
import dev.dres.data.model.template.team.TeamId

/**
 * A RESTful API representation of a [DbTeam]
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
data class ApiTeam(
    val id: TeamId? = null,
    val name: String? = null,
    val color: String? = null,
    val users: List<ApiUser> = emptyList(),
    var logoData: String? = null
) : Team {
    override val teamId: TeamId
        @JsonIgnore(true)
        get() =this.id ?: "<unspecified>"

}

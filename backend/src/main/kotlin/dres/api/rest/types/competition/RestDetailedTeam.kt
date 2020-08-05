package dres.api.rest.types.competition

import dres.api.rest.handler.UserDetails
import dres.data.model.competition.Team
import dres.mgmt.admin.UserManager

data class RestDetailedTeam(val name: String,
                            val color: String,
                            val logo: String,
                            val users: List<UserDetails>) {

    companion object {
        fun of(team: Team): RestDetailedTeam {
            return RestDetailedTeam(
                    team.name,
                    team.color,
                    team.logo,
                    team.users.map { UserDetails.of(UserManager.get(it)!!) })
        }
    }
}
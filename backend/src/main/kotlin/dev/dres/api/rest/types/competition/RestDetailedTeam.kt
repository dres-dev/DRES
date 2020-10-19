package dev.dres.api.rest.types.competition

import dev.dres.api.rest.handler.UserDetails
import dev.dres.data.model.UID
import dev.dres.data.model.competition.Team
import dev.dres.mgmt.admin.UserManager

data class RestDetailedTeam(val name: String,
                            val color: String,
                            val logo: UID,
                            val users: List<UserDetails>) {

    companion object {
        fun of(team: Team): RestDetailedTeam {
            return RestDetailedTeam(
                    team.name,
                    team.color,
                    team.logoId,
                    team.users.map { UserDetails.of(UserManager.get(it)!!) })
        }
    }
}
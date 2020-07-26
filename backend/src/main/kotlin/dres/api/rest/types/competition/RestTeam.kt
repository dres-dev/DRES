package dres.api.rest.types.competition

import dres.data.model.competition.Team

data class RestTeam(val name: String,
                    val color: String,
                    val logo: String,
                    val users: List<String>) {

    constructor(team: Team) : this(
            team.name,
            team.color,
            team.logo,
            team.users.map { it.string }
    )

}
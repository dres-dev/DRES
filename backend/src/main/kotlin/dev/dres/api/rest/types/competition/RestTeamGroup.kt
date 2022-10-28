package dev.dres.api.rest.types.competition

import dev.dres.data.model.competition.team.Team
import dev.dres.data.model.competition.TeamGroup
import dev.dres.data.model.competition.TeamGroupAggregation
import dev.dres.utilities.extensions.UID

data class RestTeamGroup(
    val uid: String? = null,
    val name: String,
    val teams: List<String>,
    val aggregation: String
) {
    fun toTeamGroup(teams: MutableList<Team>): TeamGroup = TeamGroup(
        this.uid!!.UID(),
        this.name,
        teams.filter { it.uid.string in this.teams },
        TeamGroupAggregation.valueOf(this.aggregation)
    )

    constructor(teamGroup: TeamGroup) : this(teamGroup.uid.string, teamGroup.name, teamGroup.teams.map { it.uid.string }, teamGroup.aggregation.name)

}
package dev.dres.api.rest.types.competition

import dev.dres.api.rest.types.competition.tasks.ApiTaskGroup
import dev.dres.api.rest.types.competition.tasks.ApiTaskType
import dev.dres.api.rest.types.competition.team.ApiTeam
import dev.dres.api.rest.types.competition.team.ApiTeamGroup
import dev.dres.data.model.competition.CompetitionDescription
/**
 * The RESTful API equivalent for [CompetitionDescription].
 *
 * @see CompetitionDescription
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
data class ApiCompetitionDescription(
    val id: String,
    val name: String,
    val description: String?,
    val taskTypes: List<ApiTaskType>,
    val taskGroups: List<ApiTaskGroup>,
    val teams: List<ApiTeam>,
    val teamGroups: List<ApiTeamGroup>,
    val judges: List<String>
)


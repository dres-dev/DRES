package dres.api.rest.types

import dres.data.model.competition.CompetitionDescription
import dres.data.model.competition.TaskGroup
import dres.data.model.competition.TaskType

data class RestCompetitionDescription(
        val id: String,
        val name: String,
        val description: String?,
        val taskTypes: List<TaskType>,
        val groups: List<TaskGroup>,
        val tasks: List<RestTaskDescription>,
        val teams: List<RestTeam>
) {
    constructor(description: CompetitionDescription) : this(
            description.id.string,
            description.name,
            description.description,
            description.taskTypes,
            description.groups,
            description.tasks.map { RestTaskDescription(it) },
            description.teams.map { RestTeam(it) }
    )
}
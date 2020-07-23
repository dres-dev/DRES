package dres.api.rest.types

import dres.data.model.competition.CompetitionDescription
import dres.data.model.competition.TaskGroup
import dres.data.model.competition.TaskType
import dres.data.model.competition.Team

data class RestCompetitionDescription(
        val id: String,
        val name: String,
        val description: String?,
        val taskTypes: List<TaskType>,
        val groups: MutableList<TaskGroup>,
        val tasks: MutableList<RestTaskDescription>,
        val teams: MutableList<Team>
) {
    constructor(description: CompetitionDescription) : this(
            description.id.toString(),
            description.name,
            description.description,
            description.taskTypes,
            description.groups,
            description.tasks.map { RestTaskDescription(it) }.toMutableList(),
            description.teams
    )
}
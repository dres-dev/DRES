package dres.data.model.competition

import dres.data.model.Entity

data class CompetitionDescription(override var id: Long, val name: String, val description: String?, val groups: MutableList<TaskGroup>, val tasks: MutableList<TaskDescriptionBase>, val teams: MutableList<Team>) : Entity {
    fun validate() {
        for (group in this.groups) {
            if (this.groups.map { it.name }.count { it == group.name } > 1) {
                throw IllegalArgumentException("Duplicate group with name '${group.name}'!")
            }
        }

        for (task in this.tasks) {
            if (this.tasks.map { it.name }.count { it == task.name } > 1) {
                throw IllegalArgumentException("Duplicate task with name '${task.name}'!")
            }
        }

        for (team in this.teams) {
            if (this.teams.map { it.name }.count { it == team.name } > 1) {
                throw IllegalArgumentException("Duplicate team with name '${team.name}'!")
            }
        }
    }
}
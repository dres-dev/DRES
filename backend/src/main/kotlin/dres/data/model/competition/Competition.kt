package dres.data.model.competition

import dres.data.model.Entity
import dres.data.model.competition.interfaces.TaskDescription
import kotlinx.serialization.Serializable

@Serializable
data class Competition(override var id: Long, val name: String, val description: String?, val tasks: MutableList<TaskDescription>, val teams: MutableList<Team>) : Entity {
    fun validate() {
        for (task in this.tasks) {
            if (tasks.map { it.name }.count { it == task.name } > 1) {
                throw IllegalArgumentException("Duplicate task with name '${task.name}'!")
            }
        }

        for (team in this.teams) {
            if (teams.map { it.name }.count { it == team.name } > 1) {
                throw IllegalArgumentException("Duplicate team with name '${team.name}'!")
            }
        }
    }

    fun addTask(task: TaskDescription) {
        if (tasks.map { it.name }.contains(task.name)) {
            throw IllegalArgumentException("Task with name ${task.name} already exists")
        }
        this.tasks.add(task)
    }

    fun addTeam(team: Team){
        if (teams.map { it.name }.contains(team.name)) {
            throw IllegalArgumentException("Team with name ${team.name} already exists")
        }
        this.teams.add(team)
    }
}
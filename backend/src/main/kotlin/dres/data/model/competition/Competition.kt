package dres.data.model.competition

import dres.data.model.Entity
import java.lang.IllegalArgumentException
import java.lang.Long.max

data class Competition(override var id: Long, val name: String, val description: String?, val tasks: MutableList<Task>, val teams: MutableList<Team>) : Entity {


    fun validate() {
        for (task in this.tasks) {
            if (tasks.map { it.name }.contains(task.name)) {
                throw IllegalArgumentException("Duplicate task with name '${task.name}'!")
            }
        }

        for (team in this.teams) {
            if (teams.map { it.number }.contains(team.number)) {
                throw IllegalArgumentException("Duplicate team with number '${team.number}'!")
            }
            if (teams.map { it.name }.contains(team.name)) {
                throw IllegalArgumentException("Duplicate team with name '${team.name}'!")
            }
        }
    }


    fun addTask(task: Task) {
        task.id = max(0, (tasks.maxBy { it.id }?.id ?: 0) + 1)

        if (tasks.map { it.name }.contains(task.name)) {
            throw IllegalArgumentException("Task with name ${task.name} already exists")
        }

        tasks.add(task)
    }

    fun addTeam(team: Team){
        if (teams.map { it.name }.contains(team.name)) {
            throw IllegalArgumentException("Team with name ${team.name} already exists")
        }

        if (teams.map { it.number }.contains(team.number)) {
            throw IllegalArgumentException("Team with Number ${team.number} already exists")
        }
        teams.add(team)
    }

}
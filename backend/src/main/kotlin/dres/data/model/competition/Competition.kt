package dres.data.model.competition

import dres.data.model.Entity
import java.lang.IllegalArgumentException
import java.lang.Long.max

data class Competition(override var id: Long, val name: String, val description: String?, val tasks: MutableList<Task>, val teams: MutableList<Team>) : Entity {

    fun addTask(task: Task) {
        task.id = max(0, (tasks.maxBy { it.id }?.id ?: 0) + 1)

        if (tasks.map { it.name }.contains(task.name)) {
            throw IllegalArgumentException("Task with name ${task.name} already exists")
        }

        tasks.add(task)
    }

    fun addTeam(team: Team){
        team.id = max(0, (teams.maxBy { it.id }?.id ?: 0) + 1)

        if (teams.map { it.name }.contains(team.name)) {
            throw IllegalArgumentException("Team with name ${team.name} already exists")
        }

        if (teams.map { it.number }.contains(team.number)) {
            throw IllegalArgumentException("Team with Number ${team.number} already exists")
        }

        teams.add(team)
    }

}
package dev.dres.api.rest.types.run

import dev.dres.data.model.run.interfaces.Task
import dev.dres.run.*

data class AdminRunOverview(val state: RunManagerStatus, val teamOverviews: List<TeamTaskOverview>) {

    companion object {
        fun of(run: InteractiveRunManager): AdminRunOverview {

            val teamOverviews = when (run) {
                is InteractiveSynchronousRunManager -> {
                    val overview = run.run.tasks.map { TaskRunOverview(it) }
                    run.description.teams.map {
                        TeamTaskOverview(it.uid.string, overview)
                    }
                }
                is InteractiveAsynchronousRunManager -> {
                    run.run.tasks.groupBy { it.teamId }.map { (teamId, tasks) ->
                        val overview = tasks.map { TaskRunOverview(it) }
                        TeamTaskOverview(teamId.string, overview)
                    }
                }
                else -> throw IllegalStateException("Unsupported run manager type") //should never happen
            }

            return AdminRunOverview(run.status, teamOverviews)
        }
    }

}

data class TaskRunOverview(
    val id:String,
    val name: String,
    val type: String,
    val group: String,
    val duration: Long,
    val taskId: String,
    val status: TaskRunStatus,
    val started: Long?,
    val ended: Long?) {
    constructor(task: Task) : this(
        task.description.id.string,
        task.description.name,
        task.description.taskGroup.name,
        task.description.taskType.name,
        task.description.duration,
        task.uid.string,
        task.status,
        task.started,
        task.ended)
}

data class TeamTaskOverview(val teamId: String, val tasks: List<TaskRunOverview>)

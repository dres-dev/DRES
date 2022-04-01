package dev.dres.api.rest.types.run

import dev.dres.data.model.UID
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.run.InteractiveSynchronousCompetition

/**
 * Basic and most importantly static information about the [TaskDescription]
 * of a [InteractiveSynchronousCompetition]. Since this information usually doesn't change in the course of a run, it
 * allows for local caching  and other optimizations.
 *
 * @author Ralph Gasser and Loris Sauter
 * @version 1.0.2
 */
data class TaskInfo(
        val id: String,
        val name: String,
        val taskGroup: String,
        val taskType: String,
        val duration: Long) {

    constructor(task: TaskDescription) : this(
        task.id.string,
        task.name,
        task.taskGroup.name,
        task.taskType.name,
        task.duration
    )

    companion object {
        val EMPTY_INFO = TaskInfo(UID.EMPTY.string, "N/A", "N/A", "N/A", 0)
    }
}

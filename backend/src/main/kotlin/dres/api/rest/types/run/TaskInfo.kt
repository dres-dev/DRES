package dres.api.rest.types.run

import dres.data.model.competition.TaskDescription
import dres.data.model.competition.TaskType
import dres.data.model.run.CompetitionRun

/**
 * Basic and most importantly static information about the [dres.data.model.competition.TaskDescription]
 * of a [CompetitionRun]. Since this information usually doesn't change in the course of a run, it
 * allows for local caching  and other optimizations.
 *
 * @author Ralph Gasser
 * @version 1.0.1
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
}
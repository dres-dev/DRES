package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.run.TaskStatus

data class ApiTaskOverview(
    val id:String,
    val name: String,
    val type: String,
    val group: String,
    val duration: Long,
    val taskId: String,
    val status: TaskStatus,
    val started: Long?,
    val ended: Long?) {
    constructor(task: TaskRun) : this(
        task.template.id,
        task.template.name,
        task.template.taskGroup.name,
        task.template.taskGroup.type.name,
        task.template.duration,
        task.taskId,
        task.status,
        task.started,
        task.ended)
}
package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.run.interfaces.TaskRun

data class ApiTaskOverview(
    val id: String,
    val name: String,
    val type: String,
    val group: String,
    val duration: Long,
    val taskId: String,
    val status: ApiTaskStatus,
    val started: Long?,
    val ended: Long?) {
    constructor(task: TaskRun) : this(
        task.template.id!!,
        task.template.name,
        task.template.taskGroup,
        task.template.taskType,
        task.template.duration,
        task.taskId,
        task.status,
        task.started,
        task.ended)
}
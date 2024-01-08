package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.data.model.template.task.DbTaskTemplate
import kotlinx.serialization.Serializable

/**
 * Basic and most importantly static information about a [DbTaskTemplate] relevant to the participant.
 */
@Serializable
data class ApiClientTaskTemplateInfo(
    val name: String,
    val taskGroup: String,
    val taskType: String,
    val duration: Long
) {
    constructor(task: ApiTaskTemplate) : this(
        task.name,
        task.taskGroup,
        task.taskType,
        task.duration
    )
}

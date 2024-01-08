package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.data.model.template.task.DbTaskTemplate
import kotlinx.serialization.Serializable

/**
 * Basic and most importantly static information about a [DbTaskTemplate].
 *
 * Since this information usually doesn't change in the course of a run, it allows for local caching and other optimizations.
 *
 * @author Ralph Gasser & Loris Sauter
 * @version 1.2.0
 */
@Serializable
data class ApiTaskTemplateInfo(
    val templateId: String,
    val name: String,
    val comment: String?,
    val taskGroup: String,
    val taskType: String,
    val duration: Long
) {
    constructor(task: ApiTaskTemplate) : this(
        task.id!!,
        task.name,
        task.comment,
        task.taskGroup,
        task.taskType,
        task.duration
    )
}

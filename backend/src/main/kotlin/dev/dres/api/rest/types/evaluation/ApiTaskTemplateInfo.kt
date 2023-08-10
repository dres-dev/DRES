package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.data.model.template.task.DbTaskTemplate

/**
 * Basic and most importantly static information about a [DbTaskTemplate].
 *
 * Since this information usually doesn't change in the course of a run, it allows for local caching and other optimizations.
 *
 * @author Ralph Gasser
 * @author Loris Sauter
 * @version 1.1.0
 */
data class ApiTaskTemplateInfo(val templateId: String, val name: String, val taskGroup: String, val taskType: String, val duration: Long) {

    constructor(task: ApiTaskTemplate) : this(task.id!!, task.name, task.taskGroup, task.taskType, task.duration)

    companion object {
        val EMPTY_INFO = ApiTaskTemplateInfo("", "N/A", "N/A", "N/A", 0)
    }
}

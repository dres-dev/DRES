package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.run.InteractiveSynchronousEvaluation

/**
 * Basic and most importantly static information about a [TaskTemplate].
 *
 * Since this information usually doesn't change in the course of a run, it allows for local caching and other optimizations.
 *
 * @author Ralph Gasser
 * @author Loris Sauter
 * @version 1.1.0
 */
data class ApiTaskTemplateInfo(val templateId: String, val name: String, val taskGroup: String, val taskType: String, val duration: Long) {

    constructor(task: TaskTemplate) : this(task.id, task.name, task.taskGroup.name, task.taskGroup.type.name, task.duration)

    companion object {
        val EMPTY_INFO = ApiTaskTemplateInfo("", "N/A", "N/A", "N/A", 0)
    }
}

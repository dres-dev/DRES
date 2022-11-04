package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.run.InteractiveSynchronousEvaluation

/**
 * Basic and most importantly static information about the [TaskTemplate]
 * of a [InteractiveSynchronousEvaluation]. Since this information usually doesn't change in the course of a run, it
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

    constructor(task: TaskTemplate) : this(
        task.id.string,
        task.name,
        task.taskGroup.name,
        task.taskType.name,
        task.duration
    )

    companion object {
        val EMPTY_INFO = TaskInfo(EvaluationId.EMPTY.string, "N/A", "N/A", "N/A", 0)
    }
}

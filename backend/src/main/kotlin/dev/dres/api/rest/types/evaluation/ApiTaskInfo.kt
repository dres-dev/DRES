package dev.dres.api.rest.types.evaluation

/**
 * Information of past task in a [dev.dres.data.model.run.InteractiveSynchronousEvaluation].
 * The information includes the [dev.dres.data.model.template.TaskDescription],
 * the actual task, its name, [dev.dres.data.model.template.TaskGroup],
 * [dev.dres.data.model.template.TaskType] and the number of submissions.
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
data class ApiTaskInfo(
    val taskId: String,
    val templateId: String,
    val name: String,
    val taskGroup: String,
    val taskType: String,
    val numberOfSubmissions: Int,
    val remainingTime: Long,
    val running: Boolean
)

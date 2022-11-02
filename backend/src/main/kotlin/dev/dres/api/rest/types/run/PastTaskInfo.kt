package dev.dres.api.rest.types.run

/**
 * Information of past task in a [dev.dres.data.model.run.InteractiveSynchronousEvaluation].
 * The information includes the [dev.dres.data.model.competition.TaskDescription],
 * the actual task, its name, [dev.dres.data.model.competition.TaskGroup],
 * [dev.dres.data.model.competition.TaskType] and the number of submissions.
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
data class PastTaskInfo(
    val taskId: String,
    val descriptionId: String,
    val name: String,
    val taskGroup: String,
    val taskType: String,
    val numberOfSubmissions: Int
)

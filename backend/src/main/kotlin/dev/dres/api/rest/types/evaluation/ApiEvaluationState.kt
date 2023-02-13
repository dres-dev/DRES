package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.run.InteractiveSynchronousEvaluation
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.*

/**
 * Contains the information about the state of a [InteractiveSynchronousEvaluation] and the associated [RunManager].
 *
 * This is information that changes in the course of a run and therefore must be updated frequently.
 *
 * @version 1.2.0
 */
data class ApiEvaluationState(
    val evaluationId: String,
    val evaluationStatus: ApiEvaluationStatus,
    val taskStatus: ApiTaskStatus,
    val currentTemplate: ApiTaskTemplateInfo?,
    val timeLeft: Long,
    val timeElapsed: Long
) {
    constructor(run: InteractiveRunManager, context: RunActionContext) : this(
        run.id,
        run.status.toApi(),
        when(run.currentTask(context)?.status) {
            TaskStatus.CREATED -> ApiTaskStatus.CREATED
            TaskStatus.PREPARING -> ApiTaskStatus.PREPARING
            TaskStatus.RUNNING -> ApiTaskStatus.RUNNING
            TaskStatus.ENDED -> ApiTaskStatus.ENDED
            null -> ApiTaskStatus.NO_TASK
        },
        try {
            ApiTaskTemplateInfo(run.currentTaskTemplate(context))
        } catch (e: IllegalArgumentException) {
            ApiTaskTemplateInfo.EMPTY_INFO
        },
        run.timeLeft(context) / 1000,
        run.timeElapsed(context) / 1000
    )
}

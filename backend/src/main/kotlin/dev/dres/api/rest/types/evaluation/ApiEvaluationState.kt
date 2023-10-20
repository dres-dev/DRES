package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.run.RunActionContext
import dev.dres.run.*

/**
 * Contains the information about the state of an [InteractiveRunManager].
 *
 * This is information that changes in the course of an evaluation and therefore must be updated frequently.
 *
 * @version 1.3.0
 */
data class ApiEvaluationState(
    val evaluationId: String,
    val evaluationStatus: ApiEvaluationStatus,
    val taskId: String?,
    val taskStatus: ApiTaskStatus,
    val taskTemplateId: String?,
    val timeLeft: Long,
    val timeElapsed: Long
) {
    constructor(run: InteractiveRunManager, context: RunActionContext) : this(
        run.evaluation.id,
        run.status.toApi(),
        run.currentTask(context)?.taskId,
        run.currentTask(context)?.status ?: ApiTaskStatus.NO_TASK,
        run.currentTaskTemplate(context).templateId,
        run.timeLeft(context) / 1000,
        run.timeElapsed(context) / 1000
    )
}

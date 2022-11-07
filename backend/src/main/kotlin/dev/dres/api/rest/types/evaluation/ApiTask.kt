package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.run.Task
import dev.dres.data.model.run.TaskId
import dev.dres.data.model.template.TemplateId

/**
 * The RESTful API equivalent of a [Task].
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
data class ApiTask(
    val taskId: TaskId,
    val templateId: TemplateId,
    val started: Long?,
    val ended: Long?,
    val submissions: List<ApiVerdict>
)
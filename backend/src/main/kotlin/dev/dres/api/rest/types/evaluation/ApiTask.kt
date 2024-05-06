package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.evaluation.submission.ApiAnswerSet
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.api.rest.types.evaluation.submission.ApiSubmission
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.run.TaskId
import dev.dres.data.model.template.TemplateId
import kotlinx.serialization.Serializable

/**
 * The RESTful API equivalent of a [DbTask].
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
@Serializable
data class ApiTask(
    val taskId: TaskId,
    val templateId: TemplateId,
    val started: Long?,
    val ended: Long?,
    val submissions: List<ApiSubmission>
)

package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.evaluation.submission.ApiSubmission
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.template.TemplateId
import kotlinx.serialization.Serializable

/**
 * Encodes [ApiSubmission] data for a specific [EvaluationId] and (optionally) [EvaluationId].
 *
 * @author Loris Sauter
 * @version 1.1.0
 */
@Serializable
data class ApiSubmissionInfo(val evaluationId: EvaluationId, val taskId: TemplateId, val submissions: List<ApiSubmission>)

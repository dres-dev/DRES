package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.run.EvaluationId
import dev.dres.data.model.template.TemplateId

/**
 * Encodes [ApiSubmission] data for a specific [EvaluationId] and (optionally) [EvaluationId].
 *
 * @author Loris Sauter
 * @version 1.1.0
 */
data class ApiSubmissionInfo(val evaluationId: EvaluationId, val taskId: TemplateId, val submissions: List<ApiSubmission>)

package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.run.interfaces.EvaluationId
import kotlinx.serialization.Serializable

/** Summary of a stored evaluation that is no longer running. */
@Serializable
data class ApiEndedEvaluationInfo(
    val evaluationId: EvaluationId,
    val name: String,
    val templateName: String,
    val templateDescription: String?,
    val type: ApiEvaluationType,
    val created: Long,
    val started: Long?,
    val ended: Long?,
    val taskCount: Int,
    val submissionCount: Int,
    val verdicts: ApiVerdictCounts
)

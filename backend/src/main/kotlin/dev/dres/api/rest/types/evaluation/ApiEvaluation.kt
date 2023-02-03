package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.competition.ApiEvaluationTemplate
import dev.dres.data.model.run.DbEvaluation
import dev.dres.data.model.run.EvaluationId

/**
 * The RESTful API equivalent of a [DbEvaluation].
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
data class ApiEvaluation(
    val evaluationId: EvaluationId,
    val name: String,
    val type: ApiEvaluationType,
    val template: ApiEvaluationTemplate,
    val started: Long,
    val ended: Long?,
    val tasks: List<ApiTask>
)
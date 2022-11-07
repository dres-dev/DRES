package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.competition.ApiEvaluationTemplate
import dev.dres.data.model.run.Evaluation
import dev.dres.data.model.run.EvaluationId

/**
 * The RESTful API equivalent of a [Evaluation].
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
data class ApiEvaluation(
    val evaluationId: EvaluationId,
    val name: String,
    val type: ApiRunType,
    val template: ApiEvaluationTemplate,
    val started: Long,
    val ended: Long?,
    val tasks: List<ApiTask>
)
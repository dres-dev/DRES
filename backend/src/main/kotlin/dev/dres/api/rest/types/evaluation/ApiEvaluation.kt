package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.template.ApiEvaluationTemplate
import dev.dres.data.model.run.DbEvaluation
import dev.dres.data.model.run.interfaces.Evaluation
import dev.dres.data.model.run.interfaces.EvaluationId
import kotlinx.serialization.Serializable

/**
 * The RESTful API equivalent of a [DbEvaluation].
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
@Serializable
data class ApiEvaluation(
    override val evaluationId: EvaluationId,
    val name: String,
    val type: ApiEvaluationType,
    val template: ApiEvaluationTemplate,
    val created: Long,
    val started: Long?,
    val ended: Long?,
    val tasks: List<ApiTask>
) : Evaluation

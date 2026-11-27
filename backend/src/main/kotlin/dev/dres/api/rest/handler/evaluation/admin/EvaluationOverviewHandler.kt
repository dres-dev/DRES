package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.evaluation.ApiEvaluationOverview
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.mgmt.EvaluationManager
import dev.dres.utilities.extensions.evaluationId
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class EvaluationOverviewHandler(private val store: TransientEntityStore) :
    AbstractEvaluationAdminHandler(), GetRestHandler<ApiEvaluationOverview> {
    override val route = "evaluation/admin/{evaluationId}/overview"

    @OpenApi(
        summary = "Provides a complete overview of an evaluation.",
        path = "/api/v2/evaluation/admin/{evaluationId}/overview",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID", required = true, allowEmptyValue = false),
        ],
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiEvaluationOverview::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): ApiEvaluationOverview {
        val evaluationId = ctx.evaluationId()
        val evaluationManager = getManager(evaluationId)
            ?: throw ErrorStatusException(404, "Evaluation $evaluationId not found.", ctx)
        return store.transactional(readonly = true) {
            val verdicts = EvaluationManager.verdictCountsInCurrentTransaction(evaluationId)
            ApiEvaluationOverview.of(evaluationManager, verdicts)
        }
    }
}

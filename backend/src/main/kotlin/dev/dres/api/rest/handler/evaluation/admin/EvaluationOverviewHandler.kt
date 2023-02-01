package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.evaluationId
import dev.dres.api.rest.types.evaluation.ApiEvaluationOverview
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class EvaluationOverviewHandler(store: TransientEntityStore): AbstractEvaluationAdminHandler(store), GetRestHandler<ApiEvaluationOverview> {
    override val route = "run/admin/{runId}/overview"
    @OpenApi(
        summary = "Provides a complete overview of a run.",
        path = "/api/v2/run/admin/{runId}/overview",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam("runId", String::class, "The evaluation ID", required = true, allowEmptyValue = false),
        ],
        tags = ["Competition Run Admin"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiEvaluationOverview::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): ApiEvaluationOverview {
        val evaluationId = ctx.evaluationId()
        val evaluationManager = getManager(evaluationId) ?: throw ErrorStatusException(404, "Evaluation $evaluationId not found", ctx)
        return this.store.transactional(true) {
            ApiEvaluationOverview.of(evaluationManager)
        }
    }
}
package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.evaluation.ApiEvaluationInfo
import dev.dres.api.rest.types.status.ErrorStatus
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 *
 */
class ListEvaluationInfoHandler : AbstractEvaluationViewerHandler(), GetRestHandler<List<ApiEvaluationInfo>> {

    override val route = "evaluation/info/list"

    @OpenApi(
        summary = "Lists an overview of all evaluations visible to the current user.",
        path = "/api/v2/evaluation/info/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Evaluation"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiEvaluationInfo>::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiEvaluationInfo> {
        return this.getRelevantManagers(ctx).map { manager ->
            ApiEvaluationInfo(manager)
        }
    }
}

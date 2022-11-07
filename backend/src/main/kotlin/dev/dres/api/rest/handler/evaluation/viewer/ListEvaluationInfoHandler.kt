package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.evaluation.ApiEvaluationInfo
import dev.dres.api.rest.types.status.ErrorStatus
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import jetbrains.exodus.database.TransientEntityStore

/**
 *
 */
class ListEvaluationInfoHandler(store: TransientEntityStore) : AbstractEvaluationViewerHandler(store), GetRestHandler<List<ApiEvaluationInfo>> {

    override val route = "evaluation/info/list"

    @OpenApi(
        summary = "Lists an overview of all evaluations visible to the current user.",
        path = "/api/v1/evaluation/info/list",
        tags = ["Evaluation"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiEvaluationInfo>::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiEvaluationInfo> = this.store.transactional(true) {
        this.getRelevantManagers(ctx).map { manager ->
            ApiEvaluationInfo(manager)
        }
    }
}
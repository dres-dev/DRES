package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.evaluation.ApiEvaluationState
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.run.RunActionContext
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import jetbrains.exodus.database.TransientEntityStore

/**
 *
 */
class ListEvaluationStatesHandler(store: TransientEntityStore): AbstractEvaluationViewerHandler(store), GetRestHandler<List<ApiEvaluationState>> {

    override val route = "evaluation/state/list"

    @OpenApi(
        summary = "Lists an overview of all evaluation visible to the current user.",
        path = "/api/v1/evaluation/state/list",
        tags = ["Evaluation"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiEvaluationState>::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiEvaluationState> = this.store.transactional(true) {
        this.getRelevantManagers(ctx).map {
            val rac = RunActionContext.runActionContext(ctx, it)
            ApiEvaluationState(it, rac)
        }
    }
}
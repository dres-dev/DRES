package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.evaluation.ApiEvaluationState
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import io.javalin.http.Context
import io.javalin.openapi.*

/**
 *
 */
class ListEvaluationStatesHandler: AbstractEvaluationViewerHandler(), GetRestHandler<List<ApiEvaluationState>> {

    override val route = "evaluation/state/list"

    @OpenApi(
        summary = "Lists an overview of all evaluation visible to the current user.",
        path = "/api/v2/evaluation/state/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Evaluation"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiEvaluationState>::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiEvaluationState> {
        return this.getRelevantManagers(ctx).map {
            val rac = ctx.runActionContext()
            ApiEvaluationState(it, rac)
        }
    }
}

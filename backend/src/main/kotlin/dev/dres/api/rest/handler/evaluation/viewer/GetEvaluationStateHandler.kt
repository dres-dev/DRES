package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.eligibleManagerForId
import dev.dres.api.rest.handler.evaluationId
import dev.dres.api.rest.handler.isParticipant
import dev.dres.api.rest.types.evaluation.ApiEvaluationState
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.InteractiveRunManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 *
 */
class GetEvaluationStateHandler(store: TransientEntityStore): AbstractEvaluationViewerHandler(store), GetRestHandler<ApiEvaluationState> {

    override val route = "evaluation/{evaluationId}/state"

    @OpenApi(
        summary = "Returns the state of a specific evaluation.",
        path = "/api/v2/evaluation/{evaluationId}/state",
        tags = ["Evaluation"],
        pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true)],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiEvaluationState::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiEvaluationState {
        val manager = ctx.eligibleManagerForId() as? InteractiveRunManager ?: throw ErrorStatusException(400, "Specified evaluation ${ctx.evaluationId()} does not have an evaluation state.'", ctx)
        return this.store.transactional (true) {
            if (!manager.runProperties.participantCanView && ctx.isParticipant()) {
                throw ErrorStatusException(403, "Access Denied", ctx)
            }
            val rac = RunActionContext.runActionContext(ctx, manager)
            ApiEvaluationState(manager, rac)
        }
    }
}
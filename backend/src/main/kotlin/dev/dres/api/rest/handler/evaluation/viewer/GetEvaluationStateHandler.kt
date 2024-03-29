package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.utilities.extensions.evaluationId
import dev.dres.utilities.extensions.isParticipant
import dev.dres.api.rest.types.evaluation.ApiEvaluationState
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunManagerStatus
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 *
 */
class GetEvaluationStateHandler : AbstractEvaluationViewerHandler(), GetRestHandler<ApiEvaluationState> {

    override val route = "evaluation/{evaluationId}/state"

    @OpenApi(
        summary = "Returns the state of a specific evaluation.",
        path = "/api/v2/evaluation/{evaluationId}/state",
        operationId = OpenApiOperation.AUTO_GENERATE,
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
        val manager = ctx.eligibleManagerForId<InteractiveRunManager>()
        if (manager.status == RunManagerStatus.TERMINATED) {
            throw ErrorStatusException(404, "Evaluation has ended.", ctx)
        }
        if (!manager.runProperties.participantCanView && ctx.isParticipant()) {
            throw ErrorStatusException(403, "Access Denied", ctx)
        }
        val rac = ctx.runActionContext()
        return ApiEvaluationState(manager, rac)
    }
}

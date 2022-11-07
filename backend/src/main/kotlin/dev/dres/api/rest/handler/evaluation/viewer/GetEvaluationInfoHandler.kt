package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.eligibleManagerForId
import dev.dres.api.rest.handler.isParticipant
import dev.dres.api.rest.types.evaluation.ApiEvaluationInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 *
 */
class GetEvaluationInfoHandler(store: TransientEntityStore) : AbstractEvaluationViewerHandler(store), GetRestHandler<ApiEvaluationInfo> {

    override val route = "evaluation/{evaluationId}/info"

    @OpenApi(
        summary = "Returns basic information about a specific evaluation.",
        path = "/api/v1/evaluation/{evaluationId}/info",
        tags = ["Competition Run"],
        pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true)],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiEvaluationInfo::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiEvaluationInfo {
        val manager = ctx.eligibleManagerForId()
        return this.store.transactional (true) {
            if (!manager.runProperties.participantCanView && ctx.isParticipant()) {
                throw ErrorStatusException(403, "Access Denied", ctx)
            }
            ApiEvaluationInfo(manager)
        }
    }
}
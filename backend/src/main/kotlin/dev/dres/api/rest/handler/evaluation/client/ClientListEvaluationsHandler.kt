package dev.dres.api.rest.handler.evaluation.client

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.evaluation.ApiClientEvaluationInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.run.DbEvaluation
import io.javalin.http.Context
import io.javalin.openapi.*

/**
 * A [GetRestHandler] used to list all ongoing [DbEvaluation]s available to the current user.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class ClientListEvaluationsHandler : AbstractEvaluationClientHandler(), GetRestHandler<List<ApiClientEvaluationInfo>> {

    override val route = "client/evaluation/list"

    @OpenApi(
        summary = "Lists an overview of all evaluation runs visible to the current client.",
        path = "/api/v2/client/evaluation/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Evaluation Client"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiClientEvaluationInfo>::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ],
        queryParams = [
            OpenApiParam("session", String::class, "Session Token")
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiClientEvaluationInfo> {
        return getRelevantManagers(ctx).map {
            ApiClientEvaluationInfo(it)
        }
    }
}

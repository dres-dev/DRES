package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.evaluation.ApiEndedEvaluationInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.mgmt.EvaluationManager
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiOperation
import io.javalin.openapi.OpenApiResponse

/** Lists evaluations that are stored but no longer running. */
class ListEndedEvaluationsHandler : AbstractEvaluationAdminHandler(), GetRestHandler<List<ApiEndedEvaluationInfo>> {
    override val route = "evaluation/admin/ended/list"

    @OpenApi(
        summary = "Lists stored evaluations that are no longer running.",
        path = "/api/v2/evaluation/admin/ended/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.GET],
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiEndedEvaluationInfo>::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): List<ApiEndedEvaluationInfo> = EvaluationManager.listEnded()
}

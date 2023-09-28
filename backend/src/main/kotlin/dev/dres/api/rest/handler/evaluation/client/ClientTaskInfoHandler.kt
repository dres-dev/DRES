package dev.dres.api.rest.handler.evaluation.client

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.api.rest.types.evaluation.ApiTaskTemplateInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.run.*
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [GetRestHandler] used to list get information about ongoing [Task]s.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class ClientTaskInfoHandler : AbstractEvaluationClientHandler(),
    GetRestHandler<ApiTaskTemplateInfo> {
    override val route = "client/evaluation/currentTask/{runId}"

    @OpenApi(
        summary = "Returns an overview of the currently active task for a run.",
        path = "/api/v2/client/evaluation/currentTask/{evaluationId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Evaluation Client"],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false)
        ],
        queryParams = [
            OpenApiParam("session", String::class, "Session Token")
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiTaskTemplateInfo::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiTaskTemplateInfo {
        val run = ctx.eligibleManagerForId<RunManager>()
        val rac = ctx.runActionContext()
        if (run !is InteractiveRunManager) throw ErrorStatusException(
            404,
            "Specified evaluation is not interactive.",
            ctx
        )
        val task =
            run.currentTask(rac) ?: throw ErrorStatusException(404, "Specified evaluation has no active task.", ctx)
        return ApiTaskTemplateInfo(task.template)
    }
}

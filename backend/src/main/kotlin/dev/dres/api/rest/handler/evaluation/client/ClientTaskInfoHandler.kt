package dev.dres.api.rest.handler.evaluation.client

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.api.rest.types.evaluation.ApiTaskTemplateInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.RunActionContext
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
class ClientTaskInfoHandler(store: TransientEntityStore): AbstractEvaluationClientHandler(store), GetRestHandler<ApiTaskTemplateInfo> {
    override val route = "client/evaluation/currentTask/{runId}"

    @OpenApi(
        summary = "Returns an overview of the currently active task for a run.",
        path = "/api/v2/client/evaluation/currentTask/{evaluationId}",
        tags = ["Client Run Info"],
        queryParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false)
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiTaskTemplateInfo::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiTaskTemplateInfo = this.store.transactional(true) { tx ->
        val run = ctx.eligibleManagerForId()
        val rac = RunActionContext.runActionContext(ctx, run)
        if (run !is InteractiveRunManager) throw ErrorStatusException(404, "Specified evaluation is not interactive.", ctx)
        val task = run.currentTask(rac) ?: throw ErrorStatusException(404, "Specified evaluation has no active task.", ctx)
        ApiTaskTemplateInfo(task.template)
    }
}
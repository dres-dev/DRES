package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.evaluationId
import dev.dres.api.rest.types.evaluation.ApiTaskTemplateInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [GetRestHandler] to list all past [Task]s.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ListPastTaskHandler : AbstractEvaluationAdminHandler(),
    GetRestHandler<List<ApiTaskTemplateInfo>> {
    override val route: String = "evaluation/admin/{evaluationId}/task/past/list"

    @OpenApi(
        summary = "Lists all past tasks for a given evaluation.",
        path = "/api/v2/evaluation/admin/{evaluationId}/task/past/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false)
        ],
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse(
                "200",
                [OpenApiContent(Array<ApiTaskTemplateInfo>::class)]
            ), // FIXME this handler provided information about submissions in past tasks
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): List<ApiTaskTemplateInfo> {
        val evaluationId = ctx.evaluationId()
        val runManager =
            getManager(evaluationId) ?: throw ErrorStatusException(404, "Evaluation $evaluationId not found", ctx)
        val rac = ctx.runActionContext()
        return runManager.tasks(rac).filter { it.hasEnded }.map {
            ApiTaskTemplateInfo(it.template)
        }

    }
}

package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.evaluationId
import dev.dres.api.rest.types.evaluation.ApiTaskTemplateInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.RunActionContext
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [GetRestHandler] to list all past [Task]s.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ListPastTaskHandler(store: TransientEntityStore): AbstractEvaluationAdminHandler(store), GetRestHandler<List<ApiTaskTemplateInfo>> {
    override val route: String = "evaluation/admin/{evaluationId}/task/past/list"

    @OpenApi(
        summary = "Lists all past tasks for a given evaluation.",
        path = "/api/v1/evaluation/admin/{evaluationId}/task/past/list",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false)
        ],
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiTaskTemplateInfo>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): List<ApiTaskTemplateInfo> {
        val evaluationId = ctx.evaluationId()
        val runManager = getManager(evaluationId) ?: throw ErrorStatusException(404, "Evaluation $evaluationId not found", ctx)
        return this.store.transactional (true) {
            val rac = RunActionContext.runActionContext(ctx, runManager)
            runManager.tasks(rac).filter { it.hasEnded }.map {
                ApiTaskTemplateInfo(
                    taskId = it.id,
                    templateId = it.template.id,
                    name = it.template.name,
                    taskGroup = it.template.taskGroup.name,
                    taskType = it.template.taskGroup.type.name,
                    numberOfSubmissions = it.submissions.size,
                    remainingTime = 0L,
                    running = false
                )
            }
        }
    }
}
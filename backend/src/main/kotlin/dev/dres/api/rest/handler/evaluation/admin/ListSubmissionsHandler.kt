package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.evaluationId
import dev.dres.api.rest.types.evaluation.ApiSubmissionInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.RunActionContext
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class ListSubmissionsHandler(store: TransientEntityStore): AbstractEvaluationAdminHandler(store), GetRestHandler<List<ApiSubmissionInfo>> {
    override val route: String = "evaluation/admin/{evaluationId}/submission/list/{templateId}"

    @OpenApi(
        summary = "Lists all submissions for a given evaluation and task.",
        path = "/api/v1/evaluation/admin/{evaluationId}/submission/list/{templateId}",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam("runId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false),
            OpenApiParam("templateId", String::class, "The task template ID.", required = true, allowEmptyValue = false)
        ],
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiSubmissionInfo>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): List<ApiSubmissionInfo> {
        val evaluationId = ctx.evaluationId()
        val templateId = ctx.pathParamMap()["templateId"] ?: throw ErrorStatusException(404, "Parameter 'templateId' is missing!'", ctx)
        val evaluationManager = getManager(evaluationId) ?: throw ErrorStatusException(404, "Evaluation $evaluationId not found", ctx)
        return this.store.transactional(true) {
            evaluationManager.tasks(RunActionContext.runActionContext(ctx, evaluationManager)).filter { it.template.templateId == templateId }.map {
                ApiSubmissionInfo(evaluationId, it.id, it.submissions.map { sub -> sub.toApi() })
            }
        }
    }
}
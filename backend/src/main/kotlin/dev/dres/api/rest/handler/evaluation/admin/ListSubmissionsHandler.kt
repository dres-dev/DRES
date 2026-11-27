package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.evaluation.HiddenResultsVisibility
import dev.dres.utilities.extensions.evaluationId
import dev.dres.api.rest.types.evaluation.ApiSubmissionInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.evaluation.submission.ApiSubmission
import dev.dres.api.rest.types.template.tasks.options.ApiTaskOption
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import io.javalin.http.Context
import io.javalin.openapi.*

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class ListSubmissionsHandler : AbstractEvaluationAdminHandler(),
    GetRestHandler<List<ApiSubmissionInfo>> {
    override val route: String = "evaluation/admin/{evaluationId}/submission/list/{templateId}"

    @OpenApi(
        summary = "Lists all submissions for a given evaluation and task.",
        path = "/api/v2/evaluation/admin/{evaluationId}/submission/list/{templateId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false),
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
        val templateId = ctx.pathParamMap()["templateId"] ?: throw ErrorStatusException(
            404,
            "Parameter 'templateId' is missing!'",
            ctx
        )
        val evaluationManager =
            getManager(evaluationId) ?: throw ErrorStatusException(404, "Evaluation $evaluationId not found", ctx)
        val rac = ctx.runActionContext()
        return evaluationManager.tasks(rac).filter { it.taskTemplateId == templateId }.map { task ->
            val hidden = task.isRunning && evaluationManager.template.taskTypes
                .find { it.name == task.template.taskType }
                ?.taskOptions?.contains(ApiTaskOption.HIDDEN_RESULTS) == true
            val submissions = task.getSubmissions()
            ApiSubmissionInfo(
                evaluationId,
                task.taskId,
                if (hidden) submissions.map(HiddenResultsVisibility::blind) else submissions
            )
        }
    }

}

package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.utilities.extensions.isParticipant
import dev.dres.api.rest.types.evaluation.submission.ApiSubmission
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.template.tasks.options.ApiTaskOption
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.run.InteractiveRunManager
import io.javalin.http.Context
import io.javalin.openapi.*


class GetSubmissionHistoryInfoHandler: AbstractEvaluationViewerHandler(), GetRestHandler<List<ApiSubmission>> {

    override val route = "evaluation/{evaluationId}/task/{taskId}/submission/list"

    @OpenApi(
        summary = "Returns the submissions of a specific task run, regardless of whether it is currently running or has ended.",
        path = "/api/v2/evaluation/{evaluationId}/task/{taskId}/submission/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Evaluation"],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false),
            OpenApiParam("taskId", String::class, "Task ID", required = true, allowEmptyValue = false)
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiSubmission>::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiSubmission> {
        val manager = ctx.eligibleManagerForId<InteractiveRunManager>()
        val rac = ctx.runActionContext()
        if (!manager.runProperties.participantCanView && ctx.isParticipant()) {
            throw ErrorStatusException(403, "Access denied.", ctx)
        }
        val taskId = ctx.pathParamMap()["taskId"] ?: throw ErrorStatusException(404, "Missing task id", ctx)
        val task = manager.currentTask(rac)
        return if (task?.template?.id == taskId && task.isRunning) {
            if (task.isRunning) {
                val hidden = manager.template.taskTypes.find { it.name == task.template.taskType }?.taskOptions?.contains(
                    ApiTaskOption.HIDDEN_RESULTS
                ) == true
                manager.currentSubmissions(rac).map { it.toApi(hidden) }
            } else {
                manager.taskForId(rac, taskId)?.getSubmissions() ?: emptyList()
            }
        } else {
            emptyList()
        }
    }
}


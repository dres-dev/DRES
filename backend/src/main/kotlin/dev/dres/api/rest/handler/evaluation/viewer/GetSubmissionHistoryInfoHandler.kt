package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.eligibleManagerForId
import dev.dres.api.rest.handler.evaluationId
import dev.dres.api.rest.handler.isParticipant
import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.template.task.options.TaskOption
import dev.dres.run.InteractiveRunManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.any
import kotlinx.dnq.query.filter

class GetSubmissionHistoryInfoHandler(store: TransientEntityStore): AbstractEvaluationViewerHandler(store), GetRestHandler<List<ApiSubmission>> {

    override val route = "evaluation/{evaluationId}/task/{taskRunId}/submission/list"

    @OpenApi(
        summary = "Returns the submissions of a specific task run, regardless of whether it is currently running or has ended.",
        path = "/api/v1/evaluation/{evaluationId}/task/{taskRunId}/submission/list",
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
        val manager = ctx.eligibleManagerForId() as? InteractiveRunManager ?: throw ErrorStatusException(400, "Specified evaluation ${ctx.evaluationId()} does not have an evaluation state.'", ctx)
        return this.store.transactional (true) {
            val rac = RunActionContext.runActionContext(ctx, manager)
            if (!manager.runProperties.participantCanView && ctx.isParticipant()) {
                throw ErrorStatusException(403, "Access denied.", ctx)
            }
            val taskId = ctx.pathParamMap()["taskId"] ?: throw ErrorStatusException(404, "Missing task id", ctx)
            val task = manager.currentTask(rac)
            if (task?.template?.id == taskId && task.isRunning) {
                if (task.isRunning) {
                    val hidden = manager.currentTaskTemplate(rac).taskGroup.type.options.filter { it eq  TaskOption.HIDDEN_RESULTS }.any()
                    manager.currentSubmissions(rac).map { it.toApi(hidden) }
                } else {
                    manager.taskForId(rac, taskId)?.getSubmissions()?.map { it.toApi() } ?: emptyList()
                }
            }
            emptyList()
        }
    }
}


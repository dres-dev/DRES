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
import kotlinx.dnq.query.contains

/**
 *
 */
class GetSubmissionAfterInfoHandler(store: TransientEntityStore): AbstractEvaluationViewerHandler(store), GetRestHandler<List<ApiSubmission>> {
    override val route = "evaluation/{evaluationId}/submission/list/after/{timestamp}"

    @OpenApi(
        summary = "Returns all submissions for the current task that are more recent than the provided timestamp, if a task is either running or has just ended.",
        path = "/api/v1/evaluation/{evaluationId}/submission/list/after/{timestamp}",
        tags = ["Evaluation"],
        pathParams = [
            OpenApiParam("evaluationId", String::class,"The evaluation ID.", required = true),
            OpenApiParam("timestamp", Long::class, "Timestamp that marks the lower bound for returned submissions.", required = false)
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

            val timestamp = ctx.pathParamMap().getOrDefault("timestamp", "0").toLong()
            val currentTask = manager.currentTask(rac) ?: throw ErrorStatusException(404, "No active task.", ctx)
            val blind = currentTask.template.taskGroup.type.options.contains(TaskOption.HIDDEN_RESULTS) && currentTask.isRunning
            manager.submissions(rac).filter { it.timestamp >= timestamp }.map { it.toApi(blind) }
        }
    }
}
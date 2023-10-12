package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.utilities.extensions.evaluationId
import dev.dres.utilities.extensions.isParticipant
import dev.dres.api.rest.types.evaluation.submission.ApiSubmission
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.template.tasks.options.ApiTaskOption
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.data.model.template.task.options.DbTaskOption
import dev.dres.run.InteractiveRunManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.contains

/**
 *
 */
class GetSubmissionAfterInfoHandler : AbstractEvaluationViewerHandler(), GetRestHandler<List<ApiSubmission>> {
    override val route = "evaluation/{evaluationId}/submission/list/after/{timestamp}"

    @OpenApi(
        summary = "Returns all submissions for the current task that are more recent than the provided timestamp, if a task is either running or has just ended.",
        path = "/api/v2/evaluation/{evaluationId}/submission/list/after/{timestamp}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Evaluation"],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true),
            OpenApiParam(
                "timestamp",
                Long::class,
                "Timestamp that marks the lower bound for returned submissions.",
                required = true
            )
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
        val manager = ctx.eligibleManagerForId() as? InteractiveRunManager ?: throw ErrorStatusException(
            400,
            "Specified evaluation ${ctx.evaluationId()} does not have an evaluation state.'",
            ctx
        )

        val rac = ctx.runActionContext()
        if (!manager.runProperties.participantCanView && ctx.isParticipant()) {
            throw ErrorStatusException(403, "Access denied.", ctx)
        }

        val timestamp = ctx.pathParamMap().getOrDefault("timestamp", "0").toLong()
        val currentTask = manager.currentTask(rac) ?: throw ErrorStatusException(404, "No active task.", ctx)

        val blind =
            currentTask.isRunning && manager.template.taskTypes.find { it.name == currentTask.template.taskType }?.taskOptions?.contains(
                ApiTaskOption.HIDDEN_RESULTS
            ) == true
        return manager.currentSubmissions(rac).filter { it.timestamp >= timestamp }.map { it.toApi(blind) }

    }
}

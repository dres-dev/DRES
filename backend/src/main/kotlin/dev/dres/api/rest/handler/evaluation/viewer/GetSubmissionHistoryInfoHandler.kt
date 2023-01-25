package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.RunActionContext
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

class GetSubmissionHistoryInfoHandler(store: TransientEntityStore): AbstractEvaluationViewerHandler(store), GetRestHandler<List<ApiSubmission>> {

    override val route = "evaluation/{evaluationId}/task/{taskRunId}/submission/list"

    @OpenApi(
        summary = "Returns the submissions of a specific task run, regardless of whether it is currently running or has ended.",
        path = "/api/v1/evaluation/{evaluationId}/task/{taskRunId}/submission/list",
        tags = ["Competition Run"],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false),
            OpenApiParam("taskRunId", String::class, "Task run ID")
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
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)
        val rac = RunActionContext.runActionContext(ctx, run)

        if (!run.runProperties.participantCanView && isParticipant(ctx)) {
            throw ErrorStatusException(403, "Access denied", ctx)
        }


        val taskRunId =
            ctx.pathParamMap()["taskRunId"]?.UID() ?: throw ErrorStatusException(404, "Missing task id", ctx)

        val task = run.currentTask(rac)

        return if (task?.template?.id == taskRunId && task.isRunning) {
            if (run.currentTaskDescription(rac).taskType.options.any { it.option == SimpleOption.HIDDEN_RESULTS }) {
                run.submissions(rac).map { ApiSubmission.blind(it) }
            } else {
                run.submissions(rac).map { ApiSubmission(it) }
            }
        } else {
            run.taskForId(rac, taskRunId)?.submissions?.map { ApiSubmission(it) } ?: emptyList()
        }
    }
}


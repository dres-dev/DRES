package dev.dres.api.rest.handler

import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.competition.tasks.ApiHintContent
import dev.dres.api.rest.types.competition.tasks.ApiTargetContent
import dev.dres.data.dbo.DAO
import dev.dres.data.model.Config
import dev.dres.data.model.media.MediaCollection
import dev.dres.data.model.template.options.SimpleOption
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.data.model.submissions.Submission
import dev.dres.run.TaskStatus
import dev.dres.utilities.extensions.UID
import io.javalin.http.Context
import io.javalin.openapi.*
import java.io.FileNotFoundException
import java.io.IOException




class CurrentTaskTargetHandler


class SubmissionInfoHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<ApiSubmission>> {
    override val route = "run/{runId}/submission/list"

    @OpenApi(
        summary = "Returns the submissions for the current task run, if it is either running or has just ended.",
        path = "/api/v1/run/{runId}/submission/list",
        tags = ["Competition Run"],
        pathParams = [OpenApiParam("runId", String::class, "Competition Run ID")],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiSubmission>::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiSubmission> {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)
        val rac = runActionContext(ctx, run)

        if (!run.runProperties.participantCanView && isParticipant(ctx)) {
            throw ErrorStatusException(403, "Access denied.", ctx)
        }

        fun limitSubmissions(submissions: List<Submission>, limit: Int, blind: Boolean = false): List<ApiSubmission> =
            submissions.groupBy { it.teamId }.values
                .map {
                    it.sortedBy { s -> s.timestamp }.take(limit)
                }.flatMap {
                    it.map { s ->
                        if (blind) {
                            ApiSubmission.blind(s)
                        } else {
                            ApiSubmission(s)
                        }
                    }
                }


        val limit = run.runProperties.limitSubmissionPreviews
        val blind = run.currentTaskDescription(rac).taskType.options.any { it.option == SimpleOption.HIDDEN_RESULTS }

        /* Obtain current task run and check status. */
        return if (run.currentTask(rac)?.isRunning == true) {
            if (limit > 0) {
                limitSubmissions(run.submissions(rac), limit, blind)
            } else {
                if (blind) {
                    run.submissions(rac).map { ApiSubmission.blind(it) }
                } else {
                    run.submissions(rac).map { ApiSubmission(it) }
                }
            }
        } else {
            if (limit > 0) {
                limitSubmissions(run.submissions(rac), limit, blind)
            } else {
                run.submissions(rac).map { ApiSubmission(it) }
            }
        }
    }
}

class RecentSubmissionInfoHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<ApiSubmission>> {
    override val route = "run/{runId}/submission/list/after/{timestamp}"

    @OpenApi(
        summary = "Returns the submissions for the current task that are newer than an indicated time, if it is either running or has just ended.",
        path = "/api/v1/run/{runId}/submission/list/after/{timestamp}",
        tags = ["Competition Run"],
        pathParams = [
            OpenApiParam("runId", String::class, "Competition Run ID"),
            OpenApiParam("timestamp", Long::class, "Minimum Timestamp of returned submissions.")
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
        val rac = runActionContext(ctx, run)

        if (!run.runProperties.participantCanView && isParticipant(ctx)) {
            throw ErrorStatusException(403, "Access denied", ctx)
        }


        val timestamp = ctx.pathParamMap().getOrDefault("timestamp", "0").toLong()
        return if (run.currentTask(rac)?.isRunning == true) {
            if (run.currentTaskDescription(rac).taskType.options.any { it.option == SimpleOption.HIDDEN_RESULTS }) {
                run.submissions(rac).filter { it.timestamp >= timestamp }.map { ApiSubmission.blind(it) }
            } else {
                run.submissions(rac).filter { it.timestamp >= timestamp }.map { ApiSubmission(it) }
            }
        } else {
            run.submissions(rac).filter { it.timestamp >= timestamp }.map { ApiSubmission.blind(it) }
        }
    }
}

class HistorySubmissionInfoHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<ApiSubmission>> {

    override val route = "run/{runId}/task/{taskRunId}/submission/list"

    @OpenApi(
        summary = "Returns the submissions of a specific task run, regardless of whether it is currently running or has ended.",
        path = "/api/v1/run/{runId}/task/{taskRunId}/submission/list",
        tags = ["Competition Run"],
        pathParams = [
            OpenApiParam("runId", String::class, "Competition Run ID"),
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
        val rac = runActionContext(ctx, run)

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


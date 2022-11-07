package dev.dres.api.rest.handler

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.evaluation.ApiEvaluationInfo
import dev.dres.api.rest.types.evaluation.RunState
import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.api.rest.types.evaluation.TaskInfo
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
import dev.dres.run.TaskRunStatus
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.sessionId
import io.javalin.security.RouteRole
import io.javalin.http.Context
import io.javalin.openapi.*
import java.io.FileNotFoundException
import java.io.IOException


abstract class AbstractCompetitionRunRestHandler : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.VIEWER)
    override val apiVersion = "v1"

    private fun userId(ctx: Context): EvaluationId = AccessManager.userIdForSession(ctx.sessionId())!!

    private fun isJudge(ctx: Context): Boolean {
        val roles = AccessManager.rolesOfSession(ctx.sessionId())
        return roles.contains(ApiRole.JUDGE) && !roles.contains(ApiRole.ADMIN)
    }

    fun isParticipant(ctx: Context): Boolean {
        val roles = AccessManager.rolesOfSession(ctx.sessionId())
        return roles.contains(ApiRole.PARTICIPANT) && !roles.contains(ApiRole.ADMIN)
    }



    fun runId(ctx: Context) = ctx.pathParamMap().getOrElse("runId") {
        throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx)
    }.UID()

}

class ListCompetitionRunInfosHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<ApiEvaluationInfo>> {

    override val route = "run/info/list"

    @OpenApi(
        summary = "Lists an overview of all competition runs visible to the current user",
        path = "/api/v1/run/info/list",
        tags = ["Competition Run"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiEvaluationInfo>::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiEvaluationInfo> = getRelevantManagers(ctx).map { ApiEvaluationInfo(it) }
}

class ListCompetitionRunStatesHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<RunState>> {

    override val route = "run/state/list"

    @OpenApi(
        summary = "Lists an overview of all competition runs visible to the current user",
        path = "/api/v1/run/state/list",
        tags = ["Competition Run"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<RunState>::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<RunState> =
        getRelevantManagers(ctx).map {
            val rac = runActionContext(ctx, it)
            RunState(it, rac)
        }


}


class GetCompetitionRunInfoHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<ApiEvaluationInfo> {

    override val route = "run/{runId}/info"

    @OpenApi(
        summary = "Returns a specific competition run.",
        path = "/api/v1/run/{runId}/info",
        tags = ["Competition Run"],
        pathParams = [OpenApiParam("runId", String::class, "Competition Run ID")],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiEvaluationInfo::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiEvaluationInfo {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)

        if (!run.runProperties.participantCanView && isParticipant(ctx)) {
            throw ErrorStatusException(403, "Access Denied", ctx)
        }

        return ApiEvaluationInfo(run)
    }
}

class GetCompetitionRunStateHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<RunState> {

    override val route = "run/{runId}/state"

    @OpenApi(
        summary = "Returns the state of a specific competition run.",
        path = "/api/v1/run/{runId}/state",
        tags = ["Competition Run"],
        pathParams = [OpenApiParam("runId", String::class, "Competition Run ID")],
        responses = [
            OpenApiResponse("200", [OpenApiContent(RunState::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): RunState {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)

        if (!run.runProperties.participantCanView && isParticipant(ctx)) {
            throw ErrorStatusException(403, "Access Denied", ctx)
        }

        val rac = runActionContext(ctx, run)

        return RunState(run, rac)
    }
}

class CurrentTaskInfoHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<TaskInfo> {

    override val route = "run/{runId}/task"

    @OpenApi(
        summary = "Returns the information for the current task (i.e. the one that is currently selected).",
        path = "/api/v1/run/{runId}/task",
        tags = ["Competition Run"],
        pathParams = [OpenApiParam("runId", String::class, "Competition Run ID")],
        responses = [
            OpenApiResponse("200", [OpenApiContent(TaskInfo::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): TaskInfo {

        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)

        val rac = runActionContext(ctx, run)

        if (!run.runProperties.participantCanView && isParticipant(ctx)) {
            throw ErrorStatusException(403, "Access denied.", ctx)
        }

        return TaskInfo(run.currentTaskDescription(rac))
    }
}

class CurrentTaskHintHandler(private val config: Config) : AbstractCompetitionRunRestHandler(),
    GetRestHandler<ApiHintContent> {

    override val route = "run/{runId}/hint/{taskId}"

    @OpenApi(
        summary = "Returns the task hint for the current task run (i.e. the one that is currently selected).",
        path = "/api/v1/run/{runId}/hint/{taskId}",
        tags = ["Competition Run"],
        pathParams = [
            OpenApiParam("runId", String::class, "Competition Run ID"),
            OpenApiParam("taskId", String::class, "Task Description ID")
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiHintContent::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiHintContent {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)

        if (!run.runProperties.participantCanView && isParticipant(ctx)) {
            throw ErrorStatusException(403, "Access denied.", ctx)
        }

        val taskId = ctx.pathParam("taskId").UID()

        val rac = runActionContext(ctx, run)

        val currentTaskDescription = run.currentTaskDescription(rac)

        val task = if (currentTaskDescription.id == taskId) {
            currentTaskDescription
        } else {
            run.taskForId(rac, taskId)?.template
        }

        if (task == null) { //request to a task id that is either invalid or not yet available
            throw ErrorStatusException(403, "Access denied.", ctx)
        }

        try {
            ctx.header("Cache-Control", "public, max-age=300") //can be cached for 5 minutes
            return task.toTaskHint(config)
        } catch (e: FileNotFoundException) {
            throw ErrorStatusException(404, "Query object cache file not found!", ctx)
        } catch (ioe: IOException) {
            throw ErrorStatusException(500, "Exception when reading query object cache file.", ctx)
        }
    }
}

class CurrentTaskTargetHandler(private val config: Config, private val collections: DAO<MediaCollection>) :
    AbstractCompetitionRunRestHandler(), GetRestHandler<ApiTargetContent> {

    override val route = "run/{runId}/target/{taskId}"

    @OpenApi(
        summary = "Returns the task target for the current task run (i.e. the one that is currently selected).",
        path = "/api/v1/run/{runId}/target/{taskId}",
        tags = ["Competition Run"],
        pathParams = [
            OpenApiParam("runId", String::class, "Competition Run ID"),
            OpenApiParam("taskId", String::class, "Task Description ID")
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiTargetContent::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiTargetContent {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)

        /* Test for access rights. */
        if (!run.runProperties.participantCanView && isParticipant(ctx)) {
            throw ErrorStatusException(403, "Access denied.", ctx)
        }
        val rac = runActionContext(ctx, run)

        /* Test for correct state. */
        if (run.currentTask(rac)?.status != TaskRunStatus.ENDED) {
            throw ErrorStatusException(400, "Query target can only be loaded if task has just ended.", ctx)
        }

        val taskId = ctx.pathParam("taskId").UID()

        val currentTaskDescription = run.currentTaskDescription(rac)

        val task = if (currentTaskDescription.id == taskId) {
            currentTaskDescription
        } else {
            run.taskForId(rac, taskId)?.template
        }

        if (task == null) { //request to a task id that is either invalid or not yet available
            throw ErrorStatusException(403, "Access denied.", ctx)
        }

        try {
            ctx.header("Cache-Control", "public, max-age=300") //can be cached for 5 minutes
            return task.toTaskTarget(config, collections)
        } catch (e: FileNotFoundException) {
            throw ErrorStatusException(404, "Query object cache file not found!", ctx)
        } catch (ioe: IOException) {
            throw ErrorStatusException(500, "Exception when reading query object cache file.", ctx)
        }
    }
}


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


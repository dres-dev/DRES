
package dev.dres.api.rest.handler

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.RestApiRole
import dev.dres.api.rest.types.run.RunInfo
import dev.dres.api.rest.types.run.RunState
import dev.dres.api.rest.types.run.SubmissionInfo
import dev.dres.api.rest.types.run.TaskInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.task.TaskHint
import dev.dres.api.rest.types.task.TaskTarget
import dev.dres.data.dbo.DAO
import dev.dres.data.model.Config
import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaCollection
import dev.dres.data.model.competition.options.SimpleOption
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunExecutor
import dev.dres.run.RunManagerStatus
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.sessionId
import io.javalin.core.security.RouteRole
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import java.io.FileNotFoundException
import java.io.IOException


abstract class AbstractCompetitionRunRestHandler : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<RouteRole> = setOf(RestApiRole.VIEWER)
    override val apiVersion = "v1"

    private fun userId(ctx: Context): UID = AccessManager.getUserIdForSession(ctx.sessionId())!!

    //private fun isAdmin(ctx: Context): Boolean = AccessManager.rolesOfSession(ctx.sessionId()).contains(RestApiRole.ADMIN)
    //private fun isJudge(ctx: Context): Boolean = AccessManager.rolesOfSession(ctx.sessionId()).contains(RestApiRole.JUDGE) && !AccessManager.rolesOfSession(ctx.sessionId()).contains(RestApiRole.ADMIN)
    //private fun isViewer(ctx: Context): Boolean = AccessManager.rolesOfSession(ctx.sessionId()).contains(RestApiRole.VIEWER) && !AccessManager.rolesOfSession(ctx.sessionId()).contains(RestApiRole.ADMIN)
    fun isParticipant(ctx: Context): Boolean = AccessManager.rolesOfSession(ctx.sessionId()).contains(RestApiRole.PARTICIPANT) && !AccessManager.rolesOfSession(ctx.sessionId()).contains(RestApiRole.ADMIN)

    fun getRelevantManagers(ctx: Context): List<InteractiveRunManager> {
        if (isParticipant(ctx)) {
            val userId = userId(ctx)
            return RunExecutor.managers().filterIsInstance(InteractiveRunManager::class.java).filter { m -> m.description.teams.any { it.users.contains(userId) } }
        }
        return RunExecutor.managers().filterIsInstance(InteractiveRunManager::class.java)
    }

    fun getRun(ctx: Context, runId: UID): InteractiveRunManager? {
        if (isParticipant(ctx)) {
            val userId = userId(ctx)
            val run = RunExecutor.managerForId(runId) ?: return null
            if (run is InteractiveRunManager && run.description.teams.any { it.users.contains(userId) }) {
                return run
            }
            return null
        }
        val run =  RunExecutor.managerForId(runId)
        if (run != null && run is InteractiveRunManager){
            return run
        }
        return null

    }

    fun runId(ctx: Context) = ctx.pathParamMap().getOrElse("runId") {
        throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx)
    }.UID()

}

class ListCompetitionRunInfosHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<RunInfo>> {

    override val route = "run/info/list"

    @OpenApi(
            summary = "Lists an overview of all competition runs visible to the current user",
            path = "/api/v1/run/info/list",
            tags = ["Competition Run"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<RunInfo>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): List<RunInfo> = getRelevantManagers(ctx).map { RunInfo(it) }
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
            ]
    )
    override fun doGet(ctx: Context): List<RunState> =
         getRelevantManagers(ctx).map {
             val rac = runActionContext(ctx, it)
             RunState(it, rac)
         }


}


class GetCompetitionRunInfoHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<RunInfo> {

    override val route = "run/info/{runId}"

    @OpenApi(
            summary = "Returns a specific competition run.",
            path = "/api/v1/run/info/{runId}",
            tags = ["Competition Run"],
            pathParams = [OpenApiParam("runId", UID::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(RunInfo::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): RunInfo {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)

        if (!run.description.participantCanView && isParticipant(ctx)){
            throw ErrorStatusException(403, "Access Denied", ctx)
        }

        return RunInfo(run)
    }
}

class GetCompetitionRunStateHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<RunState> {

    override val route = "run/state/{runId}"

    @OpenApi(
            summary = "Returns the state of a specific competition run.",
            path = "/api/v1/run/state/{runId}",
            tags = ["Competition Run"],
            pathParams = [OpenApiParam("runId", UID::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(RunState::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): RunState {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)

        if (!run.description.participantCanView && isParticipant(ctx)){
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
            pathParams = [OpenApiParam("runId", UID::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(TaskInfo::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): TaskInfo {

        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)

        val rac = runActionContext(ctx, run)

        if (!run.description.participantCanView && isParticipant(ctx)){
            throw ErrorStatusException(403, "Access denied.", ctx)
        }

        return TaskInfo(run.currentTaskDescription(rac))
    }
}

class CurrentTaskHintHandler(private val config: Config) : AbstractCompetitionRunRestHandler(), GetRestHandler<TaskHint> {

    override val route = "run/{runId}/hint"

    @OpenApi(
            summary = "Returns the task hint for the current task run (i.e. the one that is currently selected).",
            path = "/api/v1/run/{runId}/hint",
            tags = ["Competition Run"],
            pathParams = [OpenApiParam("runId", UID::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(TaskHint::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): TaskHint {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)

        if (!run.description.participantCanView && isParticipant(ctx)){
            throw ErrorStatusException(403, "Access denied.", ctx)
        }

        val rac = runActionContext(ctx, run)

        val task = run.currentTaskDescription(rac)
        try {
            return task.toTaskHint(config)
        } catch (e: FileNotFoundException) {
            throw ErrorStatusException(404, "Query object cache file not found!", ctx)
        } catch (ioe: IOException) {
            throw ErrorStatusException(500, "Exception when reading query object cache file.", ctx)
        }
    }
}

class CurrentTaskTargetHandler(private val config: Config, private val collections: DAO<MediaCollection>) : AbstractCompetitionRunRestHandler(), GetRestHandler<TaskTarget> {

    override val route = "run/{runId}/target"

    @OpenApi(
            summary = "Returns the task target for the current task run (i.e. the one that is currently selected).",
            path = "/api/v1/run/{runId}/target",
            tags = ["Competition Run"],
            pathParams = [OpenApiParam("runId", UID::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(TaskTarget::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): TaskTarget {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)

        /* Test for access rights. */
        if (!run.description.participantCanView && isParticipant(ctx)){
            throw ErrorStatusException(403, "Access denied.", ctx)
        }

        /* Test for correct state. */
        if (run.status != RunManagerStatus.TASK_ENDED) {
            throw ErrorStatusException(400, "Query target can only be loaded if task has just ended.", ctx)
        }

        val rac = runActionContext(ctx, run)

        /* Fetch query target and transform it. */
        val task = run.currentTaskDescription(rac)
        try {
            return task.toTaskTarget(config, collections)
        } catch (e: FileNotFoundException) {
            throw ErrorStatusException(404, "Query object cache file not found!", ctx)
        } catch (ioe: IOException) {
            throw ErrorStatusException(500, "Exception when reading query object cache file.", ctx)
        }
    }
}



class SubmissionInfoHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<SubmissionInfo>> {
    override val route = "run/{runId}/submissions"
    @OpenApi(
            summary = "Returns the submissions for the current task run, if it is either running or has just ended.",
            path = "/api/v1/run/{runId}/submissions",
            tags = ["Competition Run"],
            pathParams = [OpenApiParam("runId", UID::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<SubmissionInfo>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): List<SubmissionInfo> {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)
        val rac = runActionContext(ctx, run)

        if (!run.description.participantCanView && isParticipant(ctx)){
            throw ErrorStatusException(403, "Access denied.", ctx)
        }

        /* Obtain current task run and check status. */
        return if (run.status == RunManagerStatus.RUNNING_TASK) {
            if (run.currentTaskDescription(rac).taskType.options.any{ it.option == SimpleOption.HIDDEN_RESULTS }) {
                run.submissions(rac).map { SubmissionInfo.blind(it) }
            } else {
                run.submissions(rac).map { SubmissionInfo(it) }
            }
        } else {
            run.submissions(rac).map { SubmissionInfo(it) }
        }
    }
}

class RecentSubmissionInfoHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<SubmissionInfo>> {
    override val route = "run/{runId}/submissions/after/{timestamp}"
    @OpenApi(
            summary = "Returns the submissions for the current task that are newer than an indicated time, if it is either running or has just ended.",
            path = "/api/v1/run/{runId}/submissions/after/{timestamp}",
            tags = ["Competition Run"],
            pathParams = [
                OpenApiParam("runId", UID::class, "Competition Run ID"),
                OpenApiParam("timestamp", Long::class, "Minimum Timestamp of returned submissions.")
            ],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<SubmissionInfo>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): List<SubmissionInfo> {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)
        val rac = runActionContext(ctx, run)

        if (!run.description.participantCanView && isParticipant(ctx)){
            throw ErrorStatusException(403, "Access denied", ctx)
        }


        val timestamp = ctx.pathParamMap().getOrDefault("timestamp", "0").toLong()
        return if (run.status == RunManagerStatus.RUNNING_TASK) {
            if (run.currentTaskDescription(rac).taskType.options.any { it.option == SimpleOption.HIDDEN_RESULTS}) {
                run.submissions(rac).filter { it.timestamp >= timestamp }.map { SubmissionInfo.blind(it) }
            } else {
                run.submissions(rac).filter { it.timestamp >= timestamp }.map { SubmissionInfo(it) }
            }
        } else {
            run.submissions(rac).filter { it.timestamp >= timestamp }.map { SubmissionInfo.blind(it) }
        }
    }
}

class HistorySubmissionInfoHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<SubmissionInfo>> {

    override val route = "run/{runId}/task/{taskId}/submissions"

    @OpenApi(
            summary = "Returns the submissions of a specific task run, regardless of whether it is currently running or has ended.",
            path = "/api/v1/run/{runId}/task/{taskId}/submissions",
            tags = ["Competition Run"],
            pathParams = [
                OpenApiParam("runId", String::class, "Competition Run ID"),
                OpenApiParam("taskId", String::class, "Task run ID")
            ],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<SubmissionInfo>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): List<SubmissionInfo> {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)
        val rac = runActionContext(ctx, run)

        if (!run.description.participantCanView && isParticipant(ctx)){
            throw ErrorStatusException(403, "Access denied", ctx)
        }


        val taskId = ctx.pathParamMap()["taskId"]?.UID() ?: throw ErrorStatusException(404, "Missing task id", ctx)
        return if (run.currentTask(rac)?.description?.id == taskId && run.status == RunManagerStatus.RUNNING_TASK) {
            if (run.currentTaskDescription(rac).taskType.options.any { it.option == SimpleOption.HIDDEN_RESULTS }) {
                run.submissions(rac).map { SubmissionInfo.blind(it) }
            } else {
                run.submissions(rac).map { SubmissionInfo(it) }
            }
        } else {
            run.taskForId(rac, taskId)?.submissions?.map { SubmissionInfo(it) } ?: emptyList()
        }
    }
}


package dres.api.rest.handler

import dres.api.rest.AccessManager
import dres.api.rest.RestApiRole
import dres.api.rest.types.run.RunInfo
import dres.api.rest.types.run.RunState
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.data.model.Config
import dres.data.model.basics.media.MediaItem
import dres.data.model.competition.QueryDescription
import dres.data.model.competition.TaskDescriptionBase
import dres.data.model.competition.TaskGroup
import dres.data.model.competition.interfaces.HiddenResultsTaskDescription
import dres.data.model.competition.interfaces.TaskDescription
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import dres.run.RunExecutor
import dres.run.RunManager
import dres.run.RunManagerStatus
import dres.run.score.scoreboard.Score
import dres.run.score.scoreboard.ScoreOverview
import dres.utilities.extensions.sessionId
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.util.*


abstract class AbstractCompetitionRunRestHandler : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.VIEWER)

    private fun userId(ctx: Context): Long = AccessManager.getUserIdForSession(ctx.sessionId())!!

    //private fun isAdmin(ctx: Context): Boolean = AccessManager.rolesOfSession(ctx.sessionId()).contains(RestApiRole.ADMIN)
    //private fun isJudge(ctx: Context): Boolean = AccessManager.rolesOfSession(ctx.sessionId()).contains(RestApiRole.JUDGE) && !AccessManager.rolesOfSession(ctx.sessionId()).contains(RestApiRole.ADMIN)
    //private fun isViewer(ctx: Context): Boolean = AccessManager.rolesOfSession(ctx.sessionId()).contains(RestApiRole.VIEWER) && !AccessManager.rolesOfSession(ctx.sessionId()).contains(RestApiRole.ADMIN)
    fun isParticipant(ctx: Context): Boolean = AccessManager.rolesOfSession(ctx.sessionId()).contains(RestApiRole.PARTICIPANT) && !AccessManager.rolesOfSession(ctx.sessionId()).contains(RestApiRole.ADMIN)

    fun getRelevantManagers(ctx: Context): List<RunManager> {
        if (isParticipant(ctx)) {
            val userId = userId(ctx)
            return RunExecutor.managers().filter { it.competitionDescription.teams.any { it.users.contains(userId) } }
        }
        return RunExecutor.managers()
    }

    fun getRun(ctx: Context, runId: Long): RunManager? {
        if (isParticipant(ctx)) {
            val userId = userId(ctx)
            val run = RunExecutor.managerForId(runId) ?: return null
            if (run.competitionDescription.teams.any { it.users.contains(userId) }) {
                return run
            }
            return null
        }
        return RunExecutor.managerForId(runId)
    }

    fun runId(ctx: Context) = ctx.pathParamMap().getOrElse("runId") {
        throw ErrorStatusException(400, "Parameter 'runId' is missing!'")
    }.toLong()
}

class ListCompetitionRunInfosHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<RunInfo>> {

    override val route = "run/info"

    @OpenApi(
            summary = "Lists an overview of all competition runs visible to the current user",
            path = "/api/run/info",
            tags = ["Competition Run"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<RunInfo>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): List<RunInfo> = getRelevantManagers(ctx).map { RunInfo(it) }
}

class ListCompetitionRunStatesHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<RunState>> {

    override val route = "run/state"

    @OpenApi(
            summary = "Lists an overview of all competition runs visible to the current user",
            path = "/api/run/state",
            tags = ["Competition Run"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<RunState>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): List<RunState> = getRelevantManagers(ctx).map { RunState(it) }

}


class GetCompetitionRunInfoHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<RunInfo> {

    override val route = "run/info/:runId"

    @OpenApi(
            summary = "Returns a specific competition run.",
            path = "/api/run/info/:runId",
            tags = ["Competition Run"],
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(RunInfo::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): RunInfo {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.")

        if (!run.participantCanView && isParticipant(ctx)){
            throw ErrorStatusException(403, "Access Denied")
        }

        return RunInfo(run)
    }
}

class GetCompetitionRunStateHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<RunState> {

    override val route = "run/state/:runId"

    @OpenApi(
            summary = "Returns the state of a specific competition run.",
            path = "/api/run/state/:runId",
            tags = ["Competition Run"],
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(RunState::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): RunState {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.")

        if (!run.participantCanView && isParticipant(ctx)){
            throw ErrorStatusException(403, "Access Denied")
        }

        return RunState(run)
    }
}


class ListCompetitionScoreHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<ScoreOverview>> {

    override val route = "run/score/:runId"

    @OpenApi(
            summary = "Returns the score overviews of a specific competition run.",
            path = "/api/run/score/:runId",
            tags = ["Competition Run"],
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<ScoreOverview>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): List<ScoreOverview> {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.")
        return run.scoreboards.scoreboards.map { it.overview() }
    }
}

class CurrentTaskScoreHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<ScoreOverview> {

    override val route = "run/score/:runId/task"

    @OpenApi(
            summary = "Returns the overviews of all score boards for the current task run, if it is either running or has just ended.",
            path = "/api/run/score/:runId/task",
            tags = ["Competition Run"],
            pathParams = [OpenApiParam("runId", Long::class, "Competition run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(ScoreOverview::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): ScoreOverview {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.")

        if (!run.participantCanView && isParticipant(ctx)){
            throw ErrorStatusException(403, "Access denied.")
        }

        val scores = run.currentTaskScore?.scores() ?: throw ErrorStatusException(404, "No active task run in run $runId.")
        return ScoreOverview("task",
                run.currentTask?.taskGroup?.name,
                run.competitionDescription.teams.indices.sorted().map { Score(it, scores[it] ?: 0.0) }
        )
    }
}

data class TaskInfo(val name: String, val taskGroup: TaskGroup, val duration: Long) {
    companion object {
        fun of(task: TaskDescription): TaskInfo = TaskInfo(task.name, task.taskGroup, task.duration)
    }
}


class CurrentTaskInfoHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<TaskInfo> {

    override val route = "run/:runId/task"

    @OpenApi(
            summary = "Returns the information for the current task (i.e. the one that is currently selected).",
            path = "/api/run/:runId/task",
            tags = ["Competition Run"],
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(TaskInfo::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): TaskInfo {

        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.")

        if (!run.participantCanView && isParticipant(ctx)){
            throw ErrorStatusException(403, "Access denied.")
        }

        return TaskInfo.of(run.currentTask ?: throw ErrorStatusException(404, "Run $runId has currently no active task."))
    }
}

class CurrentQueryHandler(config: Config) : AbstractCompetitionRunRestHandler(), GetRestHandler<QueryDescription> {

    override val route = "run/:runId/query"

    private val taskCacheLocation = File(config.cachePath + "/tasks")
    @OpenApi(
            summary = "Returns the query description for the current task run (i.e. the one that is currently selected).",
            path = "/api/run/:runId/query",
            tags = ["Competition Run"],
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(QueryDescription::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): QueryDescription {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.")

        if (!run.participantCanView && isParticipant(ctx)){
            throw ErrorStatusException(403, "Access denied.")
        }

        val task = run.currentTask ?: throw ErrorStatusException(404, "Run $runId has currently no active task.")
        return when (task) { /* TODO: This could actually be a function of the TaskDescription?!. */
            is TaskDescriptionBase.KisVisualTaskDescription -> {
                val file = File(this.taskCacheLocation, task.cacheItemName())
                try {
                    return FileInputStream(file).use { imageInFile ->
                        val fileData = ByteArray(file.length().toInt())
                        imageInFile.read(fileData)
                        QueryDescription.VideoQueryDescription(task.name, Base64.getEncoder().encodeToString(fileData), Files.probeContentType(file.toPath()))
                    }
                } catch (e: FileNotFoundException) {
                    throw ErrorStatusException(404, "Query object cache file not found!")
                } catch (ioe: IOException) {
                    throw ErrorStatusException(500, "Exception when reading query object cache file.")
                }
            }
            is TaskDescriptionBase.KisTextualTaskDescription -> {
                val file = File(this.taskCacheLocation, task.cacheItemName())
                try {
                    return FileInputStream(file).use { imageInFile ->
                        val fileData = ByteArray(file.length().toInt())
                        imageInFile.read(fileData)
                        QueryDescription.TextQueryDescription(task.name, task.descriptions.mapIndexed { i, s -> QueryDescription.TextQueryDescription.TextualDescription(i * task.delay, s) }, Base64.getEncoder().encodeToString(fileData), "video/mp4")
                    }
                } catch (e: FileNotFoundException) {
                    throw ErrorStatusException(404, "Query object cache file not found!")
                } catch (ioe: IOException) {
                    throw ErrorStatusException(500, "Exception when reading query object cache file.")
                }

            }
            is TaskDescriptionBase.AvsTaskDescription -> {
                QueryDescription.TextQueryDescription(task.name, listOf(QueryDescription.TextQueryDescription.TextualDescription(0, task.description)))
            }
            else -> throw ErrorStatusException(500, "Exception when reading query object cache file.")
        }
    }
}


class SubmissionInfoHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<SubmissionInfo>> {
    override val route = "run/:runId/task/submissions"
    @OpenApi(
            summary = "Returns the submissions for the current task run, if it is either running or has just ended.",
            path = "/api/run/:runId/task/submissions",
            tags = ["Competition Run"],
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<SubmissionInfo>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): List<SubmissionInfo> {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.")

        if (!run.participantCanView && isParticipant(ctx)){
            throw ErrorStatusException(403, "Access denied.")
        }

        /* Obtain current task run and check status. */
        return if (run.status == RunManagerStatus.RUNNING_TASK) {
            if (run.currentTaskRun?.task is HiddenResultsTaskDescription) {
                run.submissions.map { SubmissionInfo.blind(it) }
            } else {
                run.submissions.map { SubmissionInfo.withId(it) }
            }
        } else {
            run.submissions.map { SubmissionInfo(it) }
        }
    }
}

class RecentSubmissionInfoHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<SubmissionInfo>> {
    override val route = "run/:runId/task/submissions/after/:timestamp"
    @OpenApi(
            summary = "Returns the submissions for the current task that are newer than an indicated time, if it is either running or has just ended.",
            path = "/api/run/:runId/task/submissions/after/:timestamp",
            tags = ["Competition Run"],
            pathParams = [
                OpenApiParam("runId", Long::class, "Competition Run ID"),
                OpenApiParam("timestamp", Long::class, "Minimum Timestamp for returned Submissions")
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
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.")

        if (!run.participantCanView && isParticipant(ctx)){
            throw ErrorStatusException(403, "Access denied")
        }

        val timestamp = ctx.pathParamMap().getOrDefault("timestamp", "0").toLong()
        return if (run.status == RunManagerStatus.RUNNING_TASK) {
            if (run.currentTaskRun?.task is HiddenResultsTaskDescription) {
                run.submissions.filter { it.timestamp >= timestamp }.map { SubmissionInfo.blind(it) } ?: emptyList()
            } else {
                run.submissions.filter { it.timestamp >= timestamp }.map { SubmissionInfo.withId(it) } ?: emptyList()
            }
        } else {
            run.submissions.filter { it.timestamp >= timestamp }.map { SubmissionInfo.blind(it) }
        }
    }
}

class PastSubmissionInfoHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<SubmissionInfo>> {
    override val route = "run/:runId/task/submissions/task/:taskId"
    @OpenApi(
            summary = "Returns the submissions of a specific task run, regardless of whether it is currently running or has ended.",
            path = "/api/run/:runId/task/submissions/task/:taskId",
            tags = ["Competition Run"],
            pathParams = [
                OpenApiParam("runId", Long::class, "Competition Run ID"),
                OpenApiParam("taskId", Int::class, "Task ID")
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
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.")

        if (!run.participantCanView && isParticipant(ctx)){
            throw ErrorStatusException(403, "Access denied")
        }

        val taskId = ctx.pathParamMap()["taskId"]?.toInt() ?: throw ErrorStatusException(404, "Missing task id")

        return if (run.currentTaskRun?.taskId == taskId && run.status == RunManagerStatus.RUNNING_TASK) {
            if (run.currentTaskRun?.task is HiddenResultsTaskDescription) {
                run.submissions.map { SubmissionInfo.blind(it) }
            } else {
                run.submissions.map { SubmissionInfo.withId(it) }
            }
        } else {
            val taskRun = run.taskRuns(taskId).lastOrNull()
            taskRun?.submissions?.map { SubmissionInfo(it) } ?: emptyList()
        }
    }
}

data class SubmissionInfo(val team: Int, val member: Long, val status: SubmissionStatus, val timestamp: Long, val id: String? = null, val item: MediaItem? = null, val start: Long? = null, val end: Long? = null) {
    constructor(submission: Submission) : this(submission.team, submission.member, submission.status, submission.timestamp, submission.uid, submission.item, submission.start, submission.end)

    companion object {
        fun blind(submission: Submission): SubmissionInfo = SubmissionInfo(submission.team, submission.member, submission.status, submission.timestamp)
        fun withId(submission: Submission): SubmissionInfo = SubmissionInfo(submission.team, submission.member, submission.status, submission.timestamp, submission.uid)
    }
}



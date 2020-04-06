package dres.api.rest.handler

import dres.api.rest.AccessManager
import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.types.status.SuccessStatus
import dres.data.model.competition.Task
import dres.data.model.competition.TaskType
import dres.data.model.competition.Team
import dres.data.model.run.SubmissionStatus
import dres.data.model.run.VBSSubmission
import dres.run.RunExecutor
import dres.run.RunManager
import dres.run.RunManagerStatus
import dres.run.ScoreOverview
import dres.utilities.extensions.errorResponse
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*

abstract class AbstractCompetitionRunRestHandler : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.VIEWER)

    private fun userId(ctx: Context): Long = AccessManager.getUserIdforSession(ctx.req.session.id)!!

    private fun isAdmin(ctx: Context): Boolean = AccessManager.rolesOfSession(ctx.req.session.id).contains(RestApiRole.ADMIN)

//    /**
//     * returns the runs visible to the current user
//     */
//    fun getRuns(ctx: Context): List<CompetitionRun> {
//        val userId = userId(ctx)
//        return runs.filter { it.competition.teams.any { it.users.contains(userId) } }
//    }

    fun getRelevantManagers(ctx: Context): List<RunManager> {
        if (isAdmin(ctx)){
            return RunExecutor.managers()
        }
        val userId = userId(ctx)
        return RunExecutor.managers().filter { it.competition.teams.any { it.users.contains(userId) } }
    }

    fun getRun(ctx: Context, runId: Long): RunManager? {
        if (isAdmin(ctx)){
            return RunExecutor.managerForId(runId)
        }
        val userId = userId(ctx)
        val run = RunExecutor.managerForId(runId) ?: return null
        if (run.competition.teams.any { it.users.contains(userId) }){
            return run
        }
        return null
    }

    fun runId(ctx: Context) = ctx.pathParamMap().getOrElse("runId") {
        throw ErrorStatusException(404, "Parameter 'runId' is missing!'")
    }.toLong()
}

data class CompetitionInfo(val id: Long, val name: String, val status: RunManagerStatus, val description: String, val currentTask: Task?, val teams: List<Team>) {
    companion object {
        fun of(run: RunManager): CompetitionInfo = CompetitionInfo(run.runId, run.name, run.status, run.competition.description ?: "", run.currentTask, run.competition.teams)
    }
}

class ListCompetitionRunsHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<CompetitionInfo>> {

    override val route = "run"

    @OpenApi(
            summary = "Lists an overview of all competition runs visible to the current user",
            path = "/api/run",
            tags = ["Competition Run"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<CompetitionInfo>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): List<CompetitionInfo> = getRelevantManagers(ctx).map { CompetitionInfo.of(it) }

}

class GetCompetitionRunHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<CompetitionInfo> {

    override val route = "run/:runId"

    @OpenApi(
            summary = "Returns a specific competition run.",
            path = "/api/run/:runId",
            tags = ["Competition Run"],
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(CompetitionInfo::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): CompetitionInfo {

        val runId = runId(ctx)

        val run = getRun(ctx, runId)

        if (run != null){
            return CompetitionInfo.of(run)
        }

        throw ErrorStatusException(404, "Run $runId not found")
    }
}

class ListCompetitionScoreHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<ScoreOverview>> {

    override val route = "run/:runId/score"

    @OpenApi(
            summary = "Returns the score overviews of a specific competition run.",
            path = "/api/run/:runId/score",
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

        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found")

        return run.scoreboards.map { it.overview() }
    }
}

data class TaskInfo(val name: String, val taskGroup: String, val type: TaskType, val duration: Long) {

    companion object{
        fun of(task: Task, duration: Long): TaskInfo = TaskInfo(task.name, task.taskGroup, task.description.taskType, duration)
    }

}

class CurrentTaskInfoHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<TaskInfo> {

    override val route = "run/:runId/task"

    @OpenApi(
            summary = "Returns the information for the current task.",
            path = "/api/run/:runId/task",
            tags = ["Competition Run"],
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(TaskInfo::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): TaskInfo {

        val runId = runId(ctx)

        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found")

        val task = run.currentTask ?: throw ErrorStatusException(404, "No active task in run $runId")

        return TaskInfo.of(task, 1000 * 60 * 5) //FIXME get task duration

    }
}

class CurrentQueryHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<Any> {

    override val route = "run/:runId/query"

    @OpenApi(
            summary = "Returns the actual query for the current task.",
            path = "/api/run/:runId/query",
            tags = ["Competition Run"],
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200"),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun get(ctx: Context) {

        try {
            val runId = runId(ctx)

            val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found")

            val task = run.currentTask ?: throw ErrorStatusException(404, "No active task in run $runId")


            //TODO return the actual content for the task
            throw ErrorStatusException(500, "not yet implemented")

        }catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        }

    }

    /* UNUSED */
    override fun doGet(ctx: Context) = ""
}


data class SubmissionInfo(val team: Int, val submissionTime: Long, val status: SubmissionStatus, val collection: String?, val item: String?, val startTime: String?, val endTime: String? = startTime)

class CurrentSubmissionInfoHandler : AbstractCompetitionRunRestHandler(), GetRestHandler<List<SubmissionInfo>> {

    override val route = "run/:runId/task/submissions" //TODO add a second handler with a time parameter to only get 'new' submissions

    @OpenApi(
            summary = "Returns the submissions to the current task.",
            path = "/api/run/:runId/task/submissions",
            tags = ["Competition Run"],
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<SubmissionInfo>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): List<SubmissionInfo> {

        val runId = runId(ctx)

        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found")


        val task = run.currentTask ?: throw ErrorStatusException(404, "No active task in run $runId")

        return if(task.description.taskType ==  TaskType.KIS_TEXTUAL) {
           run.submissions.map { SubmissionInfo(it.team, it.timestamp, SubmissionStatus.INDETERMINATE, null, null, null) }
        }else {
            run.submissions.map {
                val vbsSubmission = it as VBSSubmission //FIXME submission data class does not contain all relevant fields
                SubmissionInfo(it.team, it.timestamp, SubmissionStatus.INDETERMINATE, vbsSubmission.collection, vbsSubmission.item, vbsSubmission.start.toString(), vbsSubmission.end.toString())
            }
        }
    }
}

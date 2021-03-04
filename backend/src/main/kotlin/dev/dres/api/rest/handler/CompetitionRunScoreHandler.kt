package dev.dres.api.rest.handler

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.RestApiRole
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.UID
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunExecutor
import dev.dres.run.score.scoreboard.Score
import dev.dres.run.score.scoreboard.ScoreOverview
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.sessionId
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse

/**
 * A collection of [RestHandler]s that deal with [ScoreOverview]s for ongoing
 * [dev.dres.data.model.run.InteractiveSynchronousCompetitionRun]s.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractScoreRestHandler : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.VIEWER)

    private fun userId(ctx: Context): UID = AccessManager.getUserIdForSession(ctx.sessionId())!!

    /**
     * Checks if the current session has the [RestApiRole.PARTICIPANT].
     *
     * @param ctx The [Context] to check.
     */
    fun isParticipant(ctx: Context): Boolean = AccessManager.rolesOfSession(ctx.sessionId()).contains(RestApiRole.PARTICIPANT) && !AccessManager.rolesOfSession(ctx.sessionId()).contains(RestApiRole.ADMIN)


    fun getRun(ctx: Context, runId: UID): InteractiveRunManager? {
        if (isParticipant(ctx)) {
            val userId = userId(ctx)
            val run = RunExecutor.managerForId(runId) ?: return null
            if (run is InteractiveRunManager && run.competitionDescription.teams.any { it.users.contains(userId) }) {
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
}

/**
 * Generates and lists all [ScoreOverview]s for the provided [dev.dres.data.model.run.InteractiveSynchronousCompetitionRun].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ListCompetitionScoreHandler : AbstractScoreRestHandler(), GetRestHandler<List<ScoreOverview>> {

    override val route = "score/run/:runId"

    @OpenApi(
            summary = "Returns the score overviews of a specific competition run.",
            path = "/api/score/run/:runId",
            tags = ["Competition Run Scores"],
            pathParams = [OpenApiParam("runId", UID::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<ScoreOverview>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): List<ScoreOverview> {
        val runId = ctx.pathParamMap().getOrElse("runId") { throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx) }.UID()
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)
        return run.scoreboards.map { it.overview() }
    }
}

/**
 * Generates and lists the [ScoreOverview] for the currently active [dev.dres.data.model.run.InteractiveSynchronousCompetitionRun.TaskRun].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class CurrentTaskScoreHandler : AbstractScoreRestHandler(), GetRestHandler<ScoreOverview> {

    override val route = "score/run/:runId/current"

    @OpenApi(
            summary = "Returns the overviews of all score boards for the current task run, if it is either running or has just ended.",
            path = "/api/score/run/:runId/current",
            tags = ["Competition Run Scores"],
            pathParams = [OpenApiParam("runId", UID::class, "Competition run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(ScoreOverview::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): ScoreOverview {
        val runId = ctx.pathParamMap().getOrElse("runId") { throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx) }.UID()
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)

        if (!run.competitionDescription.participantCanView && isParticipant(ctx)) {
            throw ErrorStatusException(403, "Access denied.", ctx)
        }

        val rac = RunActionContext.runActionContext(ctx, run)

        val scores = run.currentTaskRun?.scorer?.scores() ?: throw ErrorStatusException(404, "No active task run in run $runId.", ctx)
        return ScoreOverview("task",
            run.currentTask(rac)?.taskGroup?.name,
            run.competitionDescription.teams.map { team ->
                Score(team.uid.string, scores[team.uid] ?: 0.0)
            }
        )
    }
}

/**
 * Generates and lists the [ScoreOverview] for the specified [dev.dres.data.model.run.InteractiveSynchronousCompetitionRun.TaskRun].
 * Can only be invoked by admins.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class HistoryTaskScoreHandler : AbstractScoreRestHandler(), GetRestHandler<ScoreOverview> {

    override val route = "score/run/:runId/history/:taskId"

    @OpenApi(
            summary = "Returns the overviews of all score boards for the specified task run.",
            path = "/api/score/run/:runId/history/:taskId",
            tags = ["Competition Run Scores"],
            pathParams = [
                OpenApiParam("runId", String::class, "Competition run ID"),
                OpenApiParam("taskId", String::class, "Task run ID")
            ],
            responses = [
                OpenApiResponse("200", [OpenApiContent(ScoreOverview::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): ScoreOverview {
        val runId = ctx.pathParamMap().getOrElse("runId") { throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx) }.UID()
        val taskId = ctx.pathParamMap().getOrElse("taskId") { throw ErrorStatusException(400, "Parameter 'taskId' is missing!'", ctx) }.UID()
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)

        val rac = RunActionContext.runActionContext(ctx, run)

        if (rac.isAdmin) {
            throw ErrorStatusException(403, "Access denied.", ctx)
        }



        /* Fetch the relevant scores and generate score overview. */
        val scores = run.taskRunForId(rac, taskId)?.scorer?.scores() ?: throw ErrorStatusException(404, "No task run with ID $taskId in run $runId.", ctx)
        return ScoreOverview("task",
            run.currentTask(rac)?.taskGroup?.name,
            run.competitionDescription.teams.map {
                Score(it.uid.string, scores[it.uid] ?: 0.0)
            }
        )
    }
}

class TaskScoreListCSVHandler : AbstractScoreRestHandler(), GetRestHandler<String> {

    override val route = "score/run/:runId/tasks/csv"

    override fun doGet(ctx: Context): String {

        val runId = ctx.pathParamMap().getOrElse("runId") { throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx) }.UID()
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)
        val rac = RunActionContext.runActionContext(ctx, run)

        return "task,group,team,score\n" + run.tasks(rac).filter { it.started != null}.sortedBy { it.started }.flatMap { task ->
            task.scorer.scores().map { "${task.taskDescription.name},${task.taskDescription.taskGroup.name},${run.competitionDescription.teams.find { t -> t.uid == it.key }?.name ?: "???"},${it.value}" }
        }.joinToString(separator = "\n")

    }

}

/**
 * A [GetRestHandler] that returns the names of all available scoreboards for a given run.
 */
class ListScoreboardsHandler : AbstractScoreRestHandler(), GetRestHandler<Array<String>> {
    override val route = "score/run/:runId/scoreboards"

    @OpenApi(
        summary = "Returns a list of available scoreboard names for the given run.",
        path = "/api/score/run/:runId/scoreboards",
        tags = ["Competition Run Scores"],
        pathParams = [
            OpenApiParam("runId", String::class, "ID of the competition run.", required = true)
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<String>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): Array<String> {
        val runId = ctx.pathParamMap().getOrElse("runId") { throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx) }.UID()
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)
        return run.scoreboards.map { it.name }.toTypedArray()
    }
}

/**
 * A [GetRestHandler] that returns a time series of all data points for a given run and scoreboard.
 */
class ListScoreSeriesHandler : AbstractScoreRestHandler(), GetRestHandler<List<ScoreSeries>> {
    override val route = "score/run/:runId/series/:scoreboard"

    @OpenApi(
        summary = "Returns a time series for a given run and scoreboard.",
        path = "/api/score/run/:runId/series/:scoreboard",
        tags = ["Competition Run Scores"],
        pathParams = [
            OpenApiParam("runId", String::class, "ID of the competition run.", required = true),
            OpenApiParam("scoreboard", String::class, "Name of the scoreboard to return the time series for.", required = true)
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ScoreSeries>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): List<ScoreSeries> {
        val runId = ctx.pathParamMap().getOrElse("runId") { throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx) }.UID()
        val scoreboard = ctx.pathParamMap().getOrElse("scoreboard") { throw ErrorStatusException(400, "Parameter 'scoreboard' is missing!'", ctx) }
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)
        return run.scoreHistory
            .filter { it.name == scoreboard }
            .groupBy { it.team }
            .mapValues { it.value.map { p -> ScoreSeriesPoint(p.score, p.timestamp) } }
            .map { ScoreSeries(it.key, scoreboard, it.value) }
    }
}

data class ScoreSeries(val team: String, val name: String, val points: List<ScoreSeriesPoint>)

data class ScoreSeriesPoint(val score: Double, val timestamp: Long)
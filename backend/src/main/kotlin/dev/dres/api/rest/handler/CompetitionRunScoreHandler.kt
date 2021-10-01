package dev.dres.api.rest.handler

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.RestApiRole
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.UID
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunExecutor
import dev.dres.run.score.interfaces.TeamTaskScorer
import dev.dres.run.score.scoreboard.Score
import dev.dres.run.score.scoreboard.ScoreOverview
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.sessionId
import io.javalin.core.security.RouteRole
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse

/**
 * A collection of [RestHandler]s that deal with [ScoreOverview]s for ongoing
 * [dev.dres.data.model.run.InteractiveSynchronousCompetition]s.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractScoreRestHandler : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<RouteRole> = setOf(RestApiRole.VIEWER)
    override val apiVersion = "v1"

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
}

/**
 * Generates and lists all [ScoreOverview]s for the provided [dev.dres.data.model.run.InteractiveSynchronousCompetition].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ListCompetitionScoreHandler : AbstractScoreRestHandler(), GetRestHandler<List<ScoreOverview>> {

    override val route = "score/run/{runId}"

    @OpenApi(
            summary = "Returns the score overviews of a specific competition run.",
            path = "/api/v1/score/run/{runId}",
            tags = ["Competition Run Scores"],
            pathParams = [OpenApiParam("runId", String::class, "Competition Run ID")],
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
 * Generates and lists the [ScoreOverview] for the currently active [dev.dres.data.model.run.InteractiveSynchronousCompetition.Task].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class CurrentTaskScoreHandler : AbstractScoreRestHandler(), GetRestHandler<ScoreOverview> {

    override val route = "score/run/{runId}/current"

    @OpenApi(
            summary = "Returns the overviews of all score boards for the current task run, if it is either running or has just ended.",
            path = "/api/v1/score/run/{runId}/current",
            tags = ["Competition Run Scores"],
            pathParams = [OpenApiParam("runId", String::class, "Competition run ID")],
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
        val rac = RunActionContext.runActionContext(ctx, run)

        if (!run.description.participantCanView && isParticipant(ctx)) {
            throw ErrorStatusException(403, "Access denied.", ctx)
        }

        val scorer = run.currentTask(rac)?.scorer ?: throw ErrorStatusException(404, "No active task run in run $runId.", ctx)
        val scores =  (scorer as? TeamTaskScorer)?.teamScoreMap() ?: throw ErrorStatusException(400, "Scorer has more than one score per team for run $runId.", ctx)

        return ScoreOverview("task",
            run.currentTaskDescription(rac).taskGroup.name,
            run.description.teams.map { team ->
                Score(team.uid.string, scores[team.uid] ?: 0.0)
            }
        )
    }
}

/**
 * Generates and lists the [ScoreOverview] for the specified [dev.dres.data.model.run.InteractiveSynchronousCompetition.Task].
 * Can only be invoked by admins.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class HistoryTaskScoreHandler : AbstractScoreRestHandler(), GetRestHandler<ScoreOverview> {

    override val route = "score/run/{runId}/history/{taskId}"

    @OpenApi(
            summary = "Returns the overviews of all score boards for the specified task run.",
            path = "/api/v1/score/run/{runId}/history/{taskId}",
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

        val scorer = run.currentTask(rac)?.scorer ?: throw ErrorStatusException(404, "No task run with ID $taskId in run $runId.", ctx)
        val scores =  (scorer as? TeamTaskScorer)?.teamScoreMap() ?: throw ErrorStatusException(400, "Scorer has more than one score per team for run $runId.", ctx)

        return ScoreOverview("task",
            run.currentTaskDescription(rac).taskGroup.name,
            run.description.teams.map {
                Score(it.uid.string, scores[it.uid] ?: 0.0)
            }
        )
    }
}

class TaskScoreListCSVHandler : AbstractScoreRestHandler(), GetRestHandler<String> {

    override val route = "score/run/{runId}/tasks/csv"

    @OpenApi(
        summary = "Provides a CSV with the scores for a given competition run",
        path = "/api/v1/score/run/{runId}/tasks/csv",
        tags = ["Competition Run Scores"],
        pathParams = [
            OpenApiParam("runId", String::class, "Competition run ID")
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class, type = "text/csv")]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): String {

        val runId = ctx.pathParamMap().getOrElse("runId") { throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx) }.UID()
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)
        val rac = RunActionContext.runActionContext(ctx, run)

        ctx.contentType("text/csv")

        return "task,group,team,resultName,score\n" + run.tasks(rac).filter { it.started != null}.sortedBy { it.started }.flatMap { task ->
            task.scorer.scores().map { "${task.description.name},${task.description.taskGroup.name},${run.description.teams.find { t -> t.uid == it.first }?.name ?: "???"},${it.second ?: "n/a"},${it.third}" }
        }.joinToString(separator = "\n")

    }

}

/**
 * A [GetRestHandler] that returns the names of all available scoreboards for a given run.
 */
class ListScoreboardsHandler : AbstractScoreRestHandler(), GetRestHandler<List<String>> {
    override val route = "score/run/{runId}/scoreboards"

    @OpenApi(
        summary = "Returns a list of available scoreboard names for the given run.",
        path = "/api/v1/score/run/{runId}/scoreboards",
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
    override fun doGet(ctx: Context): List<String> {
        val runId = ctx.pathParamMap().getOrElse("runId") { throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx) }.UID()
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)
        return run.scoreboards.map { it.name }
    }
}

/**
 * A [GetRestHandler] that returns a time series of all data points for a given run and scoreboard.
 */
class ListScoreSeriesHandler : AbstractScoreRestHandler(), GetRestHandler<List<ScoreSeries>> {
    override val route = "score/run/{runId}/series/{scoreboard}"

    @OpenApi(
        summary = "Returns a time series for a given run and scoreboard.",
        path = "/api/v1/score/run/{runId}/series/{scoreboard}",
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


class TeamGroupScoreHandler : AbstractScoreRestHandler(), GetRestHandler<List<TeamGroupValue>> {
    override val route = "score/run/{runId}/teamGroups"

    @OpenApi(
        summary = "Returns team group aggregated values of the current task.",
        path = "/api/v1/score/run/{runId}/teamGroups",
        tags = ["Competition Run Scores"],
        pathParams = [
            OpenApiParam("runId", String::class, "ID of the competition run.", required = true),
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ScoreSeries>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): List<TeamGroupValue> {
        val runId = ctx.pathParamMap().getOrElse("runId") { throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx) }.UID()
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found.", ctx)
        val rac = RunActionContext.runActionContext(ctx, run)

        if (!run.description.participantCanView && isParticipant(ctx)) {
            throw ErrorStatusException(403, "Access denied.", ctx)
        }

        val aggregators = run.currentTask(rac)?.teamGroupAggregators ?: throw ErrorStatusException(404, "No active task run in run $runId.", ctx)

        val teamGroups = run.description.teamGroups

        return teamGroups.map { TeamGroupValue(it.name, aggregators[it.uid]?.lastValue ?: 0.0) }
    }

}

data class TeamGroupValue(val name: String, val value: Double)

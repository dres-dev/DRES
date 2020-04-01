package dres.api.rest.handler

import dres.api.rest.AccessManager
import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.data.model.competition.Team
import dres.run.RunExecutor
import dres.run.RunManager
import dres.run.ScoreOverview
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse

abstract class AbstractCompetitionRunRestHandler() : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.VIEWER)

    private fun userId(ctx: Context): Long = AccessManager.getUserIdforSession(ctx.req.session.id)!!

//    /**
//     * returns the runs visible to the current user
//     */
//    fun getRuns(ctx: Context): List<CompetitionRun> {
//        val userId = userId(ctx)
//        return runs.filter { it.competition.teams.any { it.users.contains(userId) } }
//    }

    fun getRelevantManagers(ctx: Context): List<RunManager> {
        val userId = userId(ctx)
        return RunExecutor.managers().filter { it.competition.teams.any { it.users.contains(userId) } }
    }

    fun getRun(ctx: Context, runId: Long): RunManager? {
        val userId = userId(ctx)
        val run = RunExecutor.managerForId(runId) ?: return null
        if (run.competition.teams.any { it.users.contains(userId) }){
            return run
        }
        return null
    }
}

data class CompetitionInfo(val id: Long, val name: String, val Description: String, val teams: List<Team>) {

    companion object {
        fun of(run: RunManager): CompetitionInfo = CompetitionInfo(run.runId, run.name, run.competition.description
                ?: "", run.competition.teams)
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
    override fun doGet(ctx: Context): List<CompetitionInfo> = getRelevantManagers(ctx).map(CompetitionInfo::of)

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

        val runId = ctx.pathParamMap().getOrElse("runId") {
            throw ErrorStatusException(404, "Parameter 'runId' is missing!'")
        }.toLong()

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

        val runId = ctx.pathParamMap().getOrElse("runId") {
            throw ErrorStatusException(404, "Parameter 'runId' is missing!'")
        }.toLong()

        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Parameter 'runId' is missing!'")

        return run.scoreboards.map { it.overview() }
    }

}


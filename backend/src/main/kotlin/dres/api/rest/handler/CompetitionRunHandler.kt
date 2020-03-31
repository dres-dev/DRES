package dres.api.rest.handler

import dres.api.rest.AccessManager
import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.data.dbo.DAO
import dres.data.model.run.CompetitionRun
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse

abstract class AbstractCompetitionRunRestHandler(val runs: DAO<CompetitionRun>) : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.VIEWER)

    /**
     * returns the runs visible to the current user
     */
    fun getRuns(ctx: Context): List<CompetitionRun> {
        val userId = AccessManager.getUserIdforSession(ctx.req.session.id)!!
        return runs.filter { it.competition.teams.any { it.users.contains(userId) } }
    }
}

class ListCurrentRunsHandler(runs: DAO<CompetitionRun>) : AbstractCompetitionRunRestHandler(runs), GetRestHandler<List<CompetitionRun>> {

    override val route = "run"

    @OpenApi(
            summary = "Lists an overview of all competition runs visible to the current user",
            path = "/api/run",
            tags = ["Competition Run"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<CompetitionRun>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context) = getRuns(ctx)

}

class GetCurrentRunsHandler(runs: DAO<CompetitionRun>) : AbstractCompetitionRunRestHandler(runs), GetRestHandler<CompetitionRun> {

    override val route = "run/:runId"

    @OpenApi(
            summary = "Returns a specific competition run.",
            path = "/api/run/:runId",
            tags = ["Competition Run"],
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            responses = [
                OpenApiResponse("200", [OpenApiContent(CompetitionRun::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): CompetitionRun {

        val runId = ctx.pathParamMap().getOrElse("runId") {
            throw ErrorStatusException(404, "Parameter 'runId' is missing!'")
        }.toLong()

        val run = getRuns(ctx).find { it.id == runId }

        return run ?: throw ErrorStatusException(404, "Run $runId not found")

    }

}
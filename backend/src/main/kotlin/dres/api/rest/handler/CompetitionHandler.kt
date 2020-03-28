package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.data.dbo.DAO
import dres.data.model.competition.Competition
import dres.data.model.competition.Task
import dres.data.model.competition.Team
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import java.lang.Exception

abstract class CompetitionHandler(protected val competitions: DAO<Competition>) : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.ADMIN)

    private fun competitionId(ctx: Context): Long =
            ctx.pathParamMap().getOrElse("competitionId") {
                throw ErrorStatusException(404, "Parameter 'competitionId' is missing!'")
            }.toLong()

    private fun competitionById(id: Long): Competition =
            competitions[id] ?: throw ErrorStatusException(404, "Competition with ID $id not found.'")

    protected fun competitionFromContext(ctx: Context): Competition = competitionById(competitionId(ctx))

}

data class CompetitionOverview(val id: Long, val name: String, val description: String, val taskCount: Int, val teamCount: Int) {
    companion object {
        fun of(competition: Competition): CompetitionOverview = CompetitionOverview(competition.id, competition.name, competition.description
                ?: "", competition.tasks.size, competition.teams.size)
    }
}

class ListCompetitionHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), GetRestHandler<List<CompetitionOverview>> {

    @OpenApi(
            summary = "Lists an overview of all available competitions with basic information about their content.",
            path = "/api/competition",
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<CompetitionOverview>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context)  = competitions.map { CompetitionOverview.of(it) }

    override val route: String = "competition"
}

class GetCompetitionHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), GetRestHandler<Competition> {

    @OpenApi(
            summary = "Loads the detailed definition of a specific competition.",
            path = "/api/competition/:competitionId",
            pathParams = [OpenApiParam("competitionId", Long::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Competition::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context) = competitionFromContext(ctx)

    override val route: String = "competition/:competitionId"
}

class ListTeamHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), GetRestHandler<List<Team>> {

    override val route: String = "competition/:competitionId/team"

    @OpenApi(
            summary = "Lists the Teams of a specific competition.",
            path = "/api/competition/:competitionId/team",
            pathParams = [OpenApiParam("competitionId", Long::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<Team>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context) = competitionFromContext(ctx).teams


}

class ListTaskHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), GetRestHandler<List<Task>> {

    override val route: String = "competition/:competitionId/task"

    @OpenApi(
            summary = "Lists the Tasks of a specific competition.",
            path = "/api/competition/:competitionId/task",
            pathParams = [OpenApiParam("competitionId", Long::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<Task>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )

    override fun doGet(ctx: Context) = competitionFromContext(ctx).tasks

}

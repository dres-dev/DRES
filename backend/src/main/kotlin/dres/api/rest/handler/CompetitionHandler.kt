package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.data.dbo.DAO
import dres.data.model.competition.Competition
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse

abstract class CompetitionHandler(protected val competitions: DAO<Competition>) : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.ADMIN)

    private fun competitionId(ctx: Context): Long =
            ctx.pathParamMap().getOrElse("competitionId"){
                throw ErrorStatus("Parameter 'competitionId' is missing!'")
            }.toLong()

    private fun competitionById(id: Long): Competition =
            competitions[id] ?: throw ErrorStatus("Competition with ID $id not found.'")

    protected fun competitionFromContext(ctx: Context): Competition = competitionById(competitionId(ctx))


}

class ListCompetitionHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), GetRestHandler {

    @OpenApi(
            summary = "Lists an overview of all available competitions with basic information about their content.",
            path = "/api/competition",
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<CompetitionOverview>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun get(ctx: Context) {
        ctx.json(
            competitions.map { CompetitionOverview.of(it) }
        )
    }

    override val route: String = "competition"
}

class GetCompetitionHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), GetRestHandler {

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
    override fun get(ctx: Context) {

        val competition  = try{
            competitionFromContext(ctx)
        } catch (e: ErrorStatus){
            ctx.json(e)
            return
        }

        ctx.json(competition)
    }

    override val route: String = "competition/:competitionId"
}

data class CompetitionOverview(val id: Long, val name: String, val description: String, val taskCount: Int, val teamCount: Int) {
    companion object {
        fun of(competition: Competition) : CompetitionOverview = CompetitionOverview(competition.id, competition.name, competition.description ?: "", competition.tasks.size, competition.teams.size)
    }
}
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
}

class ListCompetitionHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), GetRestHandler {

    @OpenApi(
            summary = "Lists an overview of all available competitions with basic information about their content.",
            path = "/api/competition/list",
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

    override val route: String = "competition/list"
}

class GetCompetitionHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), GetRestHandler {

    @OpenApi(
            summary = "Loads the detailed definition of a specific competition.",
            path = "/api/competition/get/:competitionId",
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

        val competitionId = ctx.pathParamMap().getOrElse("competitionId") {
            ctx.status(400).json(ErrorStatus("Parameter 'competitionId' is missing!'"))
            return
        }.toLong()

        val competition = this.competitions[competitionId]
        if (competition == null){
            ctx.status(404).json(ErrorStatus("Competition with ID $competitionId not found.'"))
            return
        }

        ctx.json(competition)
    }

    override val route: String = "competition/get/:competition"
}

data class CompetitionOverview(val id: Long, val name: String, val description: String, val taskCount: Int, val teamCount: Int) {
    companion object {
        fun of(competition: Competition) : CompetitionOverview = CompetitionOverview(competition.id, competition.name, competition.description ?: "", competition.tasks.size, competition.teams.size)
    }
}
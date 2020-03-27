package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.data.dbo.DAO
import dres.data.model.competition.Competition
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiResponse

abstract class CompetitionHandler(protected val competitions: DAO<Competition>) : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.ADMIN)
}

class ListCompetitionHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), GetRestHandler {

    @OpenApi(
            summary = "list overview of all competitions",
            path = "/api/competition/list",
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<CompetitionOverview>::class)]),
                OpenApiResponse("401")
            ]
    )
    override fun get(ctx: Context) {
        ctx.json(
                competitions.map { CompetitionOverview.of(it) }
        )
    }

    override val route: String = "competition/list"

}

data class CompetitionOverview(val name: String, val description: String, val taskCount: Int, val teamCount: Int) {

    companion object {
        fun of(competition: Competition) : CompetitionOverview = CompetitionOverview(competition.name, competition.description ?: "", competition.tasks.size, competition.teams.size)
    }
}
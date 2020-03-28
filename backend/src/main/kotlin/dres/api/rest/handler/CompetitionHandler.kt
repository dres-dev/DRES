package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.SuccessStatus
import dres.data.dbo.DAO
import dres.data.model.competition.Competition
import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*

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

    override val route: String = "competition/get/:competitionId"
}

class CreateCompetitionHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), PostRestHandler {
    @OpenApi(
            summary = "Creates a new competition.",
            path = "/api/competition/create", method = HttpMethod.POST,
            requestBody = OpenApiRequestBody([OpenApiContent(CompetitionOverview::class)]),
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Competition::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun post(ctx: Context) {
        val createRequest = try {
            ctx.bodyAsClass(CompetitionOverview::class.java)
        }catch (e: BadRequestResponse){
            ctx.status(400).json(ErrorStatus("Invalid parameters. This is a programmers error!"))
            return
        }

        val competition = Competition(-1L, createRequest.name, createRequest.description, emptyList(), emptyList())
        val competitionId = this.competitions.append(competition)
        ctx.json(SuccessStatus("Competition with ID $competitionId was created."))
    }

    override val route: String = "competition/create"
}


class DeleteCompetitionHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), DeleteRestHandler {
    @OpenApi(
            summary = "Deletes the competition with the given competition ID.",
            path = "/api/competition/delete/:competitionId", method = HttpMethod.DELETE,
            pathParams = [OpenApiParam("competitionId", Long::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Competition::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun delete(ctx: Context) {
        val competitionId = ctx.pathParamMap().getOrElse("competitionId") {
            ctx.status(400).json(ErrorStatus("Parameter 'competitionId' is missing!'"))
            return
        }.toLong()

        val competition = this.competitions.delete(competitionId)
        if (competition == null){
            ctx.status(404).json(ErrorStatus("Competition with ID $competitionId not found.'"))
            return
        }

        ctx.json(SuccessStatus("Competition with ID $competitionId was deleted."))
    }

    override val route: String = "competition/delete/:competitionId"
}



data class CompetitionOverview(val id: Long, val name: String, val description: String, val taskCount: Int, val teamCount: Int) {
    companion object {
        fun of(competition: Competition) : CompetitionOverview = CompetitionOverview(competition.id, competition.name, competition.description ?: "", competition.tasks.size, competition.teams.size)
    }
}
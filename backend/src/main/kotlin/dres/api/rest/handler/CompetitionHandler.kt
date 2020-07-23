package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.types.RestCompetitionDescription
import dres.api.rest.types.RestTaskDescription
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.types.status.SuccessStatus
import dres.data.dbo.DAO
import dres.data.model.basics.media.MediaItem
import dres.data.model.competition.CompetitionDescription
import dres.data.model.competition.Team
import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*

abstract class CompetitionHandler(protected val competitions: DAO<CompetitionDescription>) : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.ADMIN)

    private fun competitionId(ctx: Context): Long =
            ctx.pathParamMap().getOrElse("competitionId") {
                throw ErrorStatusException(404, "Parameter 'competitionId' is missing!'")
            }.toLong()

    protected fun competitionById(id: Long): CompetitionDescription =
            competitions[id] ?: throw ErrorStatusException(404, "Competition with ID $id not found.'")

    protected fun competitionFromContext(ctx: Context): CompetitionDescription = competitionById(competitionId(ctx))

}

data class CompetitionOverview(val id: Long, val name: String, val description: String, val taskCount: Int, val teamCount: Int) {
    companion object {
        fun of(competitionDescription: CompetitionDescription): CompetitionOverview = CompetitionOverview(competitionDescription.id, competitionDescription.name, competitionDescription.description
                ?: "", competitionDescription.tasks.size, competitionDescription.teams.size)
    }
}

class ListCompetitionHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), GetRestHandler<List<CompetitionOverview>> {

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

class GetCompetitionHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), GetRestHandler<RestCompetitionDescription> {

    @OpenApi(
            summary = "Loads the detailed definition of a specific competition.",
            path = "/api/competition/:competitionId",
            pathParams = [OpenApiParam("competitionId", Long::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(RestCompetitionDescription::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context) = RestCompetitionDescription(competitionFromContext(ctx))

    override val route: String = "competition/:competitionId"
}

class ListTeamHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), GetRestHandler<List<Team>> {

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

class ListTaskHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), GetRestHandler<List<RestTaskDescription>> {

    override val route: String = "competition/:competitionId/task"

    @OpenApi(
            summary = "Lists the Tasks of a specific competition.",
            path = "/api/competition/:competitionId/task",
            pathParams = [OpenApiParam("competitionId", Long::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<RestTaskDescription>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )

    override fun doGet(ctx: Context) = competitionFromContext(ctx).tasks.map { RestTaskDescription(it) }

}

class CreateCompetitionHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), PostRestHandler<SuccessStatus> {
    @OpenApi(
            summary = "Creates a new competition.",
            path = "/api/competition", method = HttpMethod.POST,
            requestBody = OpenApiRequestBody([OpenApiContent(CompetitionOverview::class)]),
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val createRequest = try {
            ctx.bodyAsClass(CompetitionOverview::class.java)
        }catch (e: BadRequestResponse){
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!")
        }

        val competition = CompetitionDescription(-1L, createRequest.name, createRequest.description, mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())
        val competitionId = this.competitions.append(competition)
        return SuccessStatus("Competition with ID $competitionId was created.")
    }

    override val route: String = "competition"
}

class UpdateCompetitionHandler(competitions: DAO<CompetitionDescription>, val mediaItems: DAO<MediaItem>) : CompetitionHandler(competitions), PatchRestHandler<SuccessStatus> {
    @OpenApi(
            summary = "Updates an existing competition.",
            path = "/api/competition", method = HttpMethod.PATCH,
            requestBody = OpenApiRequestBody([OpenApiContent(RestCompetitionDescription::class)]),
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPatch(ctx: Context): SuccessStatus {
        val restCompetitionDescription = try {
            ctx.bodyAsClass(RestCompetitionDescription::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!")
        }

        val competition = CompetitionDescription(restCompetitionDescription, mediaItems)

        if (!this.competitions.exists(competition.id)) {
            throw ErrorStatusException(404, "Competition with ID ${competition.id} does not exist.")
        }

        try {
            competition.validate()
        }catch (e: IllegalArgumentException) {
            throw ErrorStatusException(400, e.message!!)
        }

        this.competitions.update(competition)
        return SuccessStatus("Competition with ID ${competition.id} was updated.")
    }

    override val route: String = "competition"
}


class DeleteCompetitionHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), DeleteRestHandler<SuccessStatus> {
    @OpenApi(
            summary = "Deletes the competition with the given competition ID.",
            path = "/api/competition/:competitionId", method = HttpMethod.DELETE,
            pathParams = [OpenApiParam("competitionId", Long::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doDelete(ctx: Context): SuccessStatus {
        val competitionToDelete = competitionFromContext(ctx)
        val competition = this.competitions.delete(competitionToDelete.id)
        return if (competition != null) {
            SuccessStatus("Competition with ID ${competitionToDelete.id} was deleted.")
        } else {
            throw ErrorStatusException(404, "Competition with ID ${competitionToDelete.id} not found.")
        }
    }

    override val route: String = "competition/:competitionId"
}


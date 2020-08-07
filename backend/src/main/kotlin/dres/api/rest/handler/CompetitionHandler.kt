package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.types.competition.RestCompetitionDescription
import dres.api.rest.types.competition.RestDetailedTeam
import dres.api.rest.types.competition.RestTaskDescription
import dres.api.rest.types.competition.RestTeam
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.types.status.SuccessStatus
import dres.data.dbo.DAO
import dres.data.model.UID
import dres.data.model.basics.media.MediaItem
import dres.data.model.competition.CompetitionDescription
import dres.data.model.competition.Team
import dres.mgmt.admin.UserManager
import dres.utilities.extensions.UID
import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*

abstract class CompetitionHandler(protected val competitions: DAO<CompetitionDescription>) : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.ADMIN)

    private fun competitionId(ctx: Context): UID =
            ctx.pathParamMap().getOrElse("competitionId") {
                throw ErrorStatusException(404, "Parameter 'competitionId' is missing!'")
            }.UID()

    protected fun competitionById(id: UID): CompetitionDescription =
            competitions[id] ?: throw ErrorStatusException(404, "Competition with ID $id not found.'")

    protected fun competitionFromContext(ctx: Context): CompetitionDescription = competitionById(competitionId(ctx))

}

data class CompetitionOverview(val id: UID, val name: String, val description: String, val taskCount: Int, val teamCount: Int) {
    companion object {
        fun of(competitionDescription: CompetitionDescription): CompetitionOverview = CompetitionOverview(competitionDescription.id, competitionDescription.name, competitionDescription.description
                ?: "", competitionDescription.tasks.size, competitionDescription.teams.size)
    }
}

/**
 * Data class for creation of competition
 */
data class CompetitionCreate(val name: String, val description: String)

class ListCompetitionHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), GetRestHandler<List<CompetitionOverview>> {

    @OpenApi(
            summary = "Lists an overview of all available competitions with basic information about their content.",
            path = "/api/competition/list",
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<CompetitionOverview>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context)  = competitions.map { CompetitionOverview.of(it) }

    override val route: String = "competition/list"
}

class GetCompetitionHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), GetRestHandler<RestCompetitionDescription> {

    @OpenApi(
            summary = "Loads the detailed definition of a specific competition.",
            path = "/api/competition/:competitionId",
            pathParams = [OpenApiParam("competitionId", UID::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(RestCompetitionDescription::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context) = RestCompetitionDescription.fromCompetition(competitionFromContext(ctx))

    override val route: String = "competition/:competitionId"
}

class ListTeamHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), GetRestHandler<List<Team>> {

    override val route: String = "competition/:competitionId/team/list"

    @OpenApi(
            summary = "Lists the Teams of a specific competition.",
            path = "/api/competition/:competitionId/team/list",
            pathParams = [OpenApiParam("competitionId", UID::class, "Competition ID")],
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


/**
 * REST handler to list all teams for a [CompetitionDescription], inclusive [UserDetails]
 */
class ListDetailedTeamHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), GetRestHandler<List<RestDetailedTeam>>{

    override val route: String = "competition/:competitionId/team/list/details"

    @OpenApi(
            summary="Lists the teams with their user details",
            path= "/api/competition/:competitionId/team/list/details",
            pathParams= [OpenApiParam("competitionId", UID::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<RestDetailedTeam>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context) = competitionFromContext(ctx).teams.map{ RestDetailedTeam.of(it) }

}

class ListTaskHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), GetRestHandler<List<RestTaskDescription>> {

    override val route: String = "competition/:competitionId/task/list"

    @OpenApi(
            summary = "Lists the Tasks of a specific competition.",
            path = "/api/competition/:competitionId/task/list",
            pathParams = [OpenApiParam("competitionId", UID::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<RestTaskDescription>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )

    override fun doGet(ctx: Context) = competitionFromContext(ctx).tasks.map { RestTaskDescription.fromTask(it) }

}

class CreateCompetitionHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), PostRestHandler<SuccessStatus> {
    @OpenApi(
            summary = "Creates a new competition.",
            path = "/api/competition", method = HttpMethod.POST,
            requestBody = OpenApiRequestBody([OpenApiContent(CompetitionCreate::class)]),
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
            ctx.bodyAsClass(CompetitionCreate::class.java)
        }catch (e: BadRequestResponse){
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!")
        }

        val competition = CompetitionDescription(UID.EMPTY, createRequest.name, createRequest.description, mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())
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

        val competition = restCompetitionDescription.toCompetitionDescription(mediaItems)

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
            pathParams = [OpenApiParam("competitionId", UID::class, "Competition ID")],
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


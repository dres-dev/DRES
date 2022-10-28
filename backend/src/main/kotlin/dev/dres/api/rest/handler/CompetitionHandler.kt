package dev.dres.api.rest.handler

import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.competition.RestCompetitionDescription
import dev.dres.api.rest.types.competition.RestDetailedTeam
import dev.dres.api.rest.types.competition.RestTaskDescription
import dev.dres.api.rest.types.competition.RestTeam
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.dbo.DAO
import dev.dres.data.model.Config
import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.team.Team
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.errorResponse
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import io.javalin.security.RouteRole
import java.io.IOException
import java.nio.file.Files

abstract class CompetitionHandler(protected val competitions: DAO<CompetitionDescription>) : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.ADMIN)
    override val apiVersion = "v1"

    private fun competitionId(ctx: Context): UID =
            ctx.pathParamMap().getOrElse("competitionId") {
                throw ErrorStatusException(404, "Parameter 'competitionId' is missing!'", ctx)
            }.UID()

    protected fun competitionById(id: UID, ctx: Context): CompetitionDescription =
            competitions[id] ?: throw ErrorStatusException(404, "Competition with ID $id not found.'", ctx)

    protected fun competitionFromContext(ctx: Context): CompetitionDescription = competitionById(competitionId(ctx), ctx)

}

data class CompetitionOverview(val id: String, val name: String, val description: String, val taskCount: Int, val teamCount: Int) {
    companion object {
        fun of(competitionDescription: CompetitionDescription): CompetitionOverview = CompetitionOverview(competitionDescription.id.string, competitionDescription.name, competitionDescription.description
                ?: "", competitionDescription.tasks.size, competitionDescription.teams.size)
    }
}

/**
 * Data class for creation of competition
 */
data class CompetitionCreate(val name: String, val description: String)

class GetTeamLogoHandler(val config: Config) : AbstractCompetitionRunRestHandler(), GetRestHandler<Any> {

    override val route = "competition/logo/{logoId}"
    override val apiVersion = "v1"

    //not used
    override fun doGet(ctx: Context): Any = ""

    @OpenApi(
            summary = "Returns the logo for the given logo ID.",
            path = "/api/v1/competition/logo/{logoId}",
            tags = ["Competition Run", "Media"],
            pathParams = [OpenApiParam("logoId", String::class, "The ID of the logo.")],
            responses = [OpenApiResponse("200"), OpenApiResponse("401"), OpenApiResponse("400"), OpenApiResponse("404")],
            ignore = true,
        methods = [HttpMethod.GET]
    )
    override fun get(ctx: Context) {

        /* Extract logoId. */
        val logoId = try {
            ctx.pathParamMap().getOrElse("logoId") {
                ctx.errorResponse(ErrorStatusException(400, "Parameter 'logoId' is missing!'", ctx))
                return@get
            }.UID()
        }catch (ex: java.lang.IllegalArgumentException){
            ctx.errorResponse(ErrorStatusException(400, "Could not deserialise logoId '${ctx.pathParamMap()["logoId"]}'", ctx))
            return
        }


        /* Load image and return it. */
        try {
            val image = Files.newInputStream(Team.logoPath(this.config, logoId)).use {
                it.readAllBytes()
            }
            ctx.contentType("image/png")
            ctx.result(image)
        } catch (e: IOException) {
            ctx.status(404)
            ctx.contentType("image/png")
            ctx.result(this.javaClass.getResourceAsStream("/img/missing.png")!!)
            //ctx.errorResponse(ErrorStatusException(404, "Logo file for team $logoId could not be read!", ctx))
        }
    }
}

class ListCompetitionHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), GetRestHandler<List<CompetitionOverview>> {

    @OpenApi(
            summary = "Lists an overview of all available competitions with basic information about their content.",
            path = "/api/v1/competition/list",
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<CompetitionOverview>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context)  = competitions.map { CompetitionOverview.of(it) }

    override val route: String = "competition/list"
}

class GetCompetitionHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), GetRestHandler<RestCompetitionDescription> {

    @OpenApi(
            summary = "Loads the detailed definition of a specific competition.",
            path = "/api/v1/competition/{competitionId}",
            pathParams = [OpenApiParam("competitionId", String::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(RestCompetitionDescription::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context) = RestCompetitionDescription.fromCompetition(competitionFromContext(ctx))

    override val route: String = "competition/{competitionId}"
}

class ListTeamHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), GetRestHandler<List<RestTeam>> {

    override val route: String = "competition/{competitionId}/team/list"

    @OpenApi(
            summary = "Lists the Teams of a specific competition.",
            path = "/api/v1/competition/{competitionId}/team/list",
            pathParams = [OpenApiParam("competitionId", String::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<RestTeam>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context) = competitionFromContext(ctx).teams.map { RestTeam(it) }

}


/**
 * REST handler to list all teams for a [CompetitionDescription], inclusive [UserDetails]
 */
class ListDetailedTeamHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), GetRestHandler<List<RestDetailedTeam>>{

    override val route: String = "competition/{competitionId}/team/list/details"

    @OpenApi(
            summary="Lists the teams with their user details",
            path = "/api/v1/competition/{competitionId}/team/list/details",
            pathParams= [OpenApiParam("competitionId", String::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<RestDetailedTeam>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context) = competitionFromContext(ctx).teams.map{ RestDetailedTeam.of(it) }

}

class ListTaskHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), GetRestHandler<List<RestTaskDescription>> {

    override val route: String = "competition/{competitionId}/task/list"

    @OpenApi(
            summary = "Lists the Tasks of a specific competition.",
            path = "/api/v1/competition/{competitionId}/task/list",
            pathParams = [OpenApiParam("competitionId", String::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<RestTaskDescription>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ],
        methods = [HttpMethod.GET]
    )

    override fun doGet(ctx: Context) = competitionFromContext(ctx).tasks.map { RestTaskDescription.fromTask(it) }

}

class CreateCompetitionHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), PostRestHandler<SuccessStatus> {
    @OpenApi(
            summary = "Creates a new competition.",
            path = "/api/v1/competition",
            methods = [HttpMethod.POST],
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
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        val competition = CompetitionDescription(UID.EMPTY, createRequest.name, createRequest.description, mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())
        val competitionId = this.competitions.append(competition)
        return SuccessStatus("Competition with ID ${competitionId.string} was created.")
    }

    override val route: String = "competition"
}

class UpdateCompetitionHandler(competitions: DAO<CompetitionDescription>, val config: Config, val mediaItems: DAO<MediaItem>) : CompetitionHandler(competitions), PatchRestHandler<SuccessStatus> {
    @OpenApi(
            summary = "Updates an existing competition.",
            path = "/api/v1/competition",
            methods = [HttpMethod.PATCH],
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
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        val competition = restCompetitionDescription.toCompetitionDescription(this.config, mediaItems)

        if (!this.competitions.exists(competition.id)) {
            throw ErrorStatusException(404, "Competition with ID ${competition.id.string} does not exist.", ctx)
        }

        try {
            competition.validate()
        }catch (e: IllegalArgumentException) {
            throw ErrorStatusException(400, e.message!!, ctx)
        }

        this.competitions.update(competition)
        return SuccessStatus("Competition with ID ${competition.id.string} was updated.")
    }

    override val route: String = "competition"
}


class DeleteCompetitionHandler(competitions: DAO<CompetitionDescription>) : CompetitionHandler(competitions), DeleteRestHandler<SuccessStatus> {
    @OpenApi(
            summary = "Deletes the competition with the given competition ID.",
            path = "/api/v1/competition/{competitionId}",
            methods = [HttpMethod.DELETE],
            pathParams = [OpenApiParam("competitionId", String::class, "Competition ID")],
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
            SuccessStatus("Competition with ID ${competitionToDelete.id.string} was deleted.")
        } else {
            throw ErrorStatusException(404, "Competition with ID ${competitionToDelete.id.string} not found.", ctx)
        }
    }

    override val route: String = "competition/{competitionId}"
}


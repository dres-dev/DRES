package dev.dres.api.rest.handler.template

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.template.team.Team
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query

/**
 * A [AbstractCompetitionDescriptionHandler] that can be used to list all [Team] logos.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 1.0.0
 */
class GetTeamLogoHandler(store: TransientEntityStore) : AbstractCompetitionDescriptionHandler(store), GetRestHandler<Any> {

    override val route = "template/logo/{logoId}"
    override val apiVersion = "v1"

    //not used
    override fun doGet(ctx: Context): Any = ""

    @OpenApi(
        summary = "Returns the logo for the given team ID.",
        path = "/api/v1/template/logo/{teamId}",
        tags = ["Evaluation", "Media"],
        pathParams = [OpenApiParam("teamId", String::class, "The ID of the team to list load the logo for.")],
        responses = [OpenApiResponse("200"), OpenApiResponse("401"), OpenApiResponse("400"), OpenApiResponse("404")],
        ignore = true,
        methods = [HttpMethod.GET]
    )
    override fun get(ctx: Context) {
        /* Extract logoId. */
        val teamId = ctx.pathParamMap()["teamId"]  ?: throw ErrorStatusException(400, "Parameter 'teamId' is missing!'", ctx)

        this.store.transactional(true) {
            val logo = Team.query(Team::id eq teamId).firstOrNull()?.logo
            if (logo != null) {
                ctx.contentType("image/png")
                ctx.result(logo)
            } else {
                ctx.status(404)
                ctx.contentType("image/png")
                ctx.result(this.javaClass.getResourceAsStream("/img/missing.png")!!)
            }
        }
    }
}
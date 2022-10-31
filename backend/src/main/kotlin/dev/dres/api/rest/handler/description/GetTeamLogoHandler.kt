package dev.dres.api.rest.handler.description

import dev.dres.api.rest.handler.AbstractCompetitionRunRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.competition.team.Team
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.errorResponse
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse
import jetbrains.exodus.database.TransientEntityStore
import java.io.IOException
import java.nio.file.Files

/**
 * A [AbstractCompetitionRunRestHandler] that can be used to list all [Team] logos.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 1.0.0
 */
class GetTeamLogoHandler(store: TransientEntityStore) : AbstractCompetitionDescriptionHandler(store), GetRestHandler<Any> {

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
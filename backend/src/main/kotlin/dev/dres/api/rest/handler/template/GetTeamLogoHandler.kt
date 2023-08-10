package dev.dres.api.rest.handler.template

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.template.team.DbTeam
import dev.dres.mgmt.TemplateManager
import io.javalin.http.Context
import io.javalin.openapi.*
import io.javalin.security.RouteRole
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query

/**
 * A [AbstractEvaluationTemplateHandler] that can be used to list all [DbTeam] logos.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 1.0.0
 */
class GetTeamLogoHandler : AbstractEvaluationTemplateHandler(), GetRestHandler<Any> {

    override val route = "template/logo/{teamId}"
    override val apiVersion = "v2"

    //not used
    override fun doGet(ctx: Context): Any = ""

    /** All authorised users can access the team logo. */
    override val permittedRoles: Set<RouteRole> =
        setOf(ApiRole.PARTICIPANT, ApiRole.VIEWER, ApiRole.JUDGE, ApiRole.ADMIN)

    @OpenApi(
        summary = "Returns the logo for the given team ID.",
        path = "/api/v2/template/logo/{teamId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Evaluation", "Media"],
        pathParams = [OpenApiParam("teamId", String::class, "The ID of the team to list load the logo for.")],
        responses = [OpenApiResponse("200"), OpenApiResponse("401"), OpenApiResponse("400"), OpenApiResponse("404")],
        ignore = true,
        methods = [HttpMethod.GET]
    )
    override fun get(ctx: Context) {
        /* Extract logoId. */
        val teamId =
            ctx.pathParamMap()["teamId"] ?: throw ErrorStatusException(400, "Parameter 'teamId' is missing!'", ctx)
        ctx.contentType("image/png")

        val logo = TemplateManager.getTeamLogo(teamId)
        if (logo != null) {
            ctx.result(logo)
        } else {
            ctx.status(404)
            ctx.result(this.javaClass.getResourceAsStream("/img/missing.png")!!)
        }

    }
}

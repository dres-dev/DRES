package dev.dres.api.rest.handler.users

import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class ShowCurrentSessionHandler : AbstractUserHandler(), GetRestHandler<SessionToken>, AccessManagedRestHandler {
    override val route = "user/session"

    /** [ShowCurrentUserHandler] can be used by [ApiRole.ADMIN], [[ApiRole.VIEWER], [ApiRole.PARTICIPANT]*/
    override val permittedRoles = setOf(ApiRole.VIEWER, ApiRole.ADMIN, ApiRole.PARTICIPANT)


    @OpenApi(
        summary = "Get current sessionId",
        path = "/api/v2/user/session",
        tags = ["User"],
        queryParams = [
            OpenApiParam("session", String::class, "Session Token")
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SessionToken::class)]),
            OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): SessionToken = ctx.sessionToken() ?: "n/a"
}
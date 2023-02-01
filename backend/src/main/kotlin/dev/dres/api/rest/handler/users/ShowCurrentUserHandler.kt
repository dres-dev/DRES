package dev.dres.api.rest.handler.users

import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class ShowCurrentUserHandler : AbstractUserHandler(), GetRestHandler<ApiUser>, AccessManagedRestHandler {
    override val route = "user"

    /** [ShowCurrentUserHandler] can be used by [ApiRole.ADMIN], [[ApiRole.VIEWER], [ApiRole.PARTICIPANT]*/
    override val permittedRoles = setOf(ApiRole.VIEWER, ApiRole.ADMIN, ApiRole.PARTICIPANT)

    @OpenApi(
        summary = "Get information about the current user.",
        path = "/api/v2/user",
        tags = ["User"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiUser::class)]),
            OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiUser {
        val user = userFromSession(ctx).toApi()
        user.sessionId = ctx.sessionToken()
        return user
    }
}
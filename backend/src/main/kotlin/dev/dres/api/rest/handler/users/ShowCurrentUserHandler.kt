package dev.dres.api.rest.handler.users

import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.users.UserDetails
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
class ShowCurrentUserHandler : AbstractUserHandler(), GetRestHandler<UserDetails>, AccessManagedRestHandler {
    override val route = "user"

    /** [ShowCurrentUserHandler] can be used by [ApiRole.ADMIN], [[ApiRole.VIEWER], [ApiRole.PARTICIPANT]*/
    override val permittedRoles = setOf(ApiRole.VIEWER, ApiRole.ADMIN, ApiRole.PARTICIPANT)

    @OpenApi(
        summary = "Get information about the current user.",
        path = "/api/v1/user",
        tags = ["User"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(UserDetails::class)]),
            OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): UserDetails = UserDetails.create(userFromSession(ctx), ctx)
}
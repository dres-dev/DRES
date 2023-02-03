package dev.dres.api.rest.handler.users

import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.admin.DbUser
import io.javalin.http.Context
import io.javalin.openapi.*

/**
 * An [AbstractUserHandler] to show [DbUser] details.
 *
 * @author Loris Sauter
 * @version 2.0.0
 */
class UserDetailsHandler : AbstractUserHandler(), GetRestHandler<ApiUser>, AccessManagedRestHandler {
    override val route = "user/{userId}"

    /** [UserDetailsHandler] requires [ApiRole.ADMIN]. */
    override val permittedRoles = ApiRole.values().toSet()

    @OpenApi(
        summary = "Gets details of the user with the given id.",
        path = "/api/v2/user/{userId}",
        pathParams = [
            OpenApiParam("userId", String::class, "User's UID")
        ],
        tags = ["User"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiUser::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)], description = "If the user could not be found."),
            OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context) = userFromContext(ctx).toApi()


}
package dev.dres.api.rest.handler.users

import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.users.UserDetails
import dev.dres.data.model.admin.User
import dev.dres.mgmt.admin.UserManager
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse

/**
 * An [AbstractUserHandler] to list all [User]s.
 *
 * @author Loris Sauter
 * @version 2.0.0
 */
class ListUsersHandler: AbstractUserHandler(), GetRestHandler<List<UserDetails>>, AccessManagedRestHandler {

    override val route = "user/list"

    /** All [UserDetailsHandler] requires [ApiRole.ADMIN]. */
    override val permittedRoles = setOf(ApiRole.ADMIN)

    @OpenApi(
        summary = "Lists all available users.",
        path = "/api/v1/user/list",
        tags = ["User"],
        responses = [OpenApiResponse("200", [OpenApiContent(Array<UserDetails>::class)])],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context) = UserManager.list().map(UserDetails.Companion::of)
}
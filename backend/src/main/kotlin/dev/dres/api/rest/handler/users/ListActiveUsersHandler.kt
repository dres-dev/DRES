package dev.dres.api.rest.handler.users

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.admin.User
import dev.dres.mgmt.admin.UserManager
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse

/**
 * An [AbstractUserHandler] to list all [User]s that are currently logged in.
 *
 * @author Loris Sauter
 * @version 2.0.0
 */
class ListActiveUsersHandler : GetRestHandler<List<ApiUser>>, AccessManagedRestHandler {
    override val permittedRoles = setOf(ApiRole.ADMIN)

    /** All [UserDetailsHandler] requires [ApiRole.ADMIN]. */
    override val route = "user/session/active/list"

    override val apiVersion = "v1"

    @OpenApi(
        summary = "Get details of all current user sessions",
        path = "/api/v1/user/session/active/list",
        tags = ["User"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiUser>::class)]),
            OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiUser> = AccessManager.currentSessions.map { session ->
        AccessManager.userIdForSession(session)?.let {
            UserManager.get(id = it)
        }?.let {
            it.toApi()
        } ?: return@map ApiUser("??", "??", ApiRole.VIEWER, session)
    }
}
package dev.dres.api.rest.handler.users

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.users.UserDetails
import dev.dres.data.model.UID
import dev.dres.data.model.admin.Role
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
class ListActiveUsersHandler() : GetRestHandler<List<UserDetails>>, AccessManagedRestHandler {
    override val permittedRoles = setOf(ApiRole.ADMIN)

    /** All [UserDetailsHandler] requires [ApiRole.ADMIN]. */
    override val route = "user/session/active/list"

    override val apiVersion = "v1"


    @OpenApi(
        summary = "Get details of all current user sessions",
        path = "/api/v1/user/session/active/list",
        tags = ["User"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<UserDetails>::class)]),
            OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<UserDetails> = AccessManager.currentSessions.map { session ->
        AccessManager.userIdForSession(session)?.let {
            UserManager.get(id = it)
        }?.let {
            UserDetails.of(it)
        } ?: return@map UserDetails(UID.EMPTY.string, "??", Role.VIEWER, session)
    }
}
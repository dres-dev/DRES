package dev.dres.api.rest.handler.users

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.admin.DbUser
import dev.dres.mgmt.admin.DbUserManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * An [AbstractUserHandler] to list all [DbUser]s that are currently logged in.
 *
 * @author Loris Sauter
 * @version 2.0.0
 */
class ListActiveUsersHandler(private val store: TransientEntityStore) : GetRestHandler<List<ApiUser>>, AccessManagedRestHandler {
    override val permittedRoles = setOf(ApiRole.ADMIN)

    /** All [UserDetailsHandler] requires [ApiRole.ADMIN]. */
    override val route = "user/session/active/list"

    override val apiVersion = "v2"

    @OpenApi(
        summary = "Get details of all current user sessions",
        path = "/api/v2/user/session/active/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["User"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiUser>::class)]),
            OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiUser> = this.store.transactional {
        AccessManager.currentSessions.map { session ->
            AccessManager.userIdForSession(session)?.let {
                DbUserManager.get(id = it)
            }?.toApi() ?: return@map ApiUser("??", "??", ApiRole.VIEWER, session)
        }
    }
}

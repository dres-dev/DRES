package dev.dres.api.rest.handler.users

import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.admin.DbUser
import dev.dres.mgmt.admin.UserManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * An [AbstractUserHandler] to list all [DbUser]s.
 *
 * @author Loris Sauter
 * @version 2.0.0
 */
class ListUsersHandler(): AbstractUserHandler(), GetRestHandler<List<ApiUser>>, AccessManagedRestHandler {

    override val route = "user/list"

    /** All [UserDetailsHandler] requires [ApiRole.ADMIN]. */
    override val permittedRoles = setOf(ApiRole.ADMIN)

    @OpenApi(
        summary = "Lists all available users.",
        path = "/api/v2/user/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["User"],
        responses = [OpenApiResponse("200", [OpenApiContent(Array<ApiUser>::class)])],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context) = UserManager.list()
}

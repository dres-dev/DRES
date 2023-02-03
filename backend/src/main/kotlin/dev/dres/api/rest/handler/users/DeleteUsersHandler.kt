package dev.dres.api.rest.handler.users

import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.DeleteRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.admin.DbUser
import dev.dres.mgmt.admin.UserManager
import io.javalin.http.Context
import io.javalin.openapi.*

/**
 * An [AbstractUserHandler] to delete [DbUser]s.
 *
 * @author Loris Sauter
 * @version 2.0.0
 */
class DeleteUsersHandler : AbstractUserHandler(), DeleteRestHandler<ApiUser>, AccessManagedRestHandler {
    override val permittedRoles = setOf(ApiRole.ADMIN)

    /** [DeleteUsersHandler] requires [ApiRole.ADMIN]. */
    override val route = "user/{userId}"

    @OpenApi(
        summary = "Deletes the specified user. Requires ADMIN privileges",
        path = "/api/v2/user/{userId}", methods = [HttpMethod.DELETE],
        pathParams = [OpenApiParam("userId", Long::class, "User ID")],
        tags = ["User"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiUser::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)], description = "If the user could not be found"),
            OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doDelete(ctx: Context): ApiUser {
        val user = userFromContext(ctx)
        if (UserManager.delete(id = user.id)) {
            return user.toApi()
        } else {
            throw ErrorStatusException(500, "Could not delete the user (${user.id})", ctx)
        }
    }
}
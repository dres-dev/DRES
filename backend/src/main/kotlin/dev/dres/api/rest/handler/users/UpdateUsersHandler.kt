package dev.dres.api.rest.handler.users

import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.PatchRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.api.rest.types.users.ApiUserRequest
import dev.dres.data.model.admin.DbUser
import dev.dres.mgmt.admin.UserManager
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*

/**
 * An [AbstractUserHandler] to update an existing [DbUser]s.
 *
 * @author Loris Sauter
 * @version 2.0.0
 */
class UpdateUsersHandler() : AbstractUserHandler(), PatchRestHandler<ApiUser>, AccessManagedRestHandler {

    /** [UpdateUsersHandler] can be used by [ApiRole.ADMIN], [[ApiRole.VIEWER], [ApiRole.PARTICIPANT]*/
    override val permittedRoles = setOf(ApiRole.VIEWER, ApiRole.ADMIN, ApiRole.PARTICIPANT)

    override val route = "user/{userId}"

    @OpenApi(
        summary = "Updates the specified user, if it exists. Anyone is allowed to update their data, however only ADMINs are allowed to update anyone.",
        path = "/api/v2/user/{userId}", methods = [HttpMethod.PATCH],
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [OpenApiParam("userId", String::class, "User ID", required = true)],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiUserRequest::class)]),
        tags = ["User"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiUser::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPatch(ctx: Context): ApiUser {
        val request = try {
            ctx.bodyAsClass(ApiUserRequest::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }


        /* Fetch existing objects. */
        val user = userFromContext(ctx)
        val caller = userFromSession(ctx)

        if (caller.role == ApiRole.ADMIN || user.id == caller.id) {

            val sanitized = if (caller.role == ApiRole.ADMIN) {
                request
            } else { //non admins can only change their passwords, nothing else
                request.copy(username = user.username!!, role = user.role)
            }

            val updated = UserManager.update(id = user.id, request = sanitized)
            if (updated != null) {
                return updated
            } else {
                throw ErrorStatusException(500, "Could not update user!", ctx)
            }
        } else {
            throw ErrorStatusException(
                403,
                "You do not have permissions to edit user (${user.id}) as ${caller.id}!",
                ctx
            )
        }

    }
}

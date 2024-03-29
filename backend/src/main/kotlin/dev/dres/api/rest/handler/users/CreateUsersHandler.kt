package dev.dres.api.rest.handler.users

import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.api.rest.types.users.ApiUserRequest
import dev.dres.data.model.admin.Password
import dev.dres.data.model.admin.DbUser
import dev.dres.mgmt.admin.UserManager
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*

/**
 * An [AbstractUserHandler] to create new [DbUser]s.
 *
 * @author Loris Sauter
 * @version 2.0.0
 */
class CreateUsersHandler() : AbstractUserHandler(), PostRestHandler<ApiUser>, AccessManagedRestHandler {
    override val route = "user"

    /** [CreateUsersHandler] requires [ApiRole.ADMIN]. */
    override val permittedRoles = setOf(ApiRole.ADMIN)

    @OpenApi(
        summary = "Creates a new user, if the username is not already taken. Requires ADMIN privileges",
        path = "/api/v2/user", methods = [HttpMethod.POST],
        operationId = OpenApiOperation.AUTO_GENERATE,
        requestBody = OpenApiRequestBody([OpenApiContent(ApiUserRequest::class)]),
        tags = ["User"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiUser::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)], description = "If the username is already taken"),
            OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): ApiUser {
        val req = try {
            ctx.bodyAsClass(ApiUserRequest::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        if (req.password == null )
            throw ErrorStatusException(400, "Invalid parameters. Password must consist of at least ${DbUser.MIN_LENGTH_PASSWORD} characters.", ctx)
        if (req.username.length < DbUser.MIN_LENGTH_USERNAME)
            throw ErrorStatusException(400, "Invalid parameters. Username must consist of at least ${DbUser.MIN_LENGTH_USERNAME} characters.", ctx)
        if (req.role == null)
            throw ErrorStatusException(400, "Invalid parameters. Role must be defined.", ctx)

        try {
            return UserManager.create(req.username, Password.Plain(req.password).hash(), req.role)
        } catch (e: Exception) {
            throw ErrorStatusException(400, e.message ?: "could not create user", ctx)
        }

    }
}

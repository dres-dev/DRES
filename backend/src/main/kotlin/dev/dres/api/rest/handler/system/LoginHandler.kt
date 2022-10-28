package dev.dres.api.rest.handler.system

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.handler.RestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.admin.Password
import dev.dres.data.model.audit.AuditLogSource
import dev.dres.mgmt.admin.UserManager
import dev.dres.mgmt.admin.UserManager.getMatchingUser
import dev.dres.run.audit.AuditLogger
import dev.dres.utilities.extensions.sessionId
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*

/**
 * A [GetRestHandler] that handles user-requests to login.
 *
 * @version 1.0.0
 * @author Luca Rossetto
 */
class LoginHandler : RestHandler, PostRestHandler<UserDetails> {

    override val apiVersion = "v1"

    data class LoginRequest(var username: String, var password: String)

    @OpenApi(summary = "Sets roles for session based on user account and returns a session cookie.",
        path = "/api/v1/login",
        methods = [HttpMethod.POST],
        tags = ["User"],
        requestBody = OpenApiRequestBody([OpenApiContent(LoginRequest::class)]),
        responses = [
            OpenApiResponse("200", [OpenApiContent(UserDetails::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context) : UserDetails{

        val loginRequest = try {
            ctx.bodyAsClass(LoginRequest::class.java)
        }catch (e: BadRequestResponse){
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error.", ctx)
        }

        /* Validate login request. */
        val username = loginRequest.username
        val password = Password.Plain(loginRequest.password)
        val user = getMatchingUser(username, password) ?: throw ErrorStatusException(401, "Invalid credentials. Please try again!", ctx)

        /* Begin user session. */
        AccessManager.registerUserForSession(ctx.sessionId(), user)
        AuditLogger.login(user.userId, AuditLogSource.REST, ctx.sessionId())

        return UserDetails.create(UserManager.get(username)!!, ctx)
    }

    override val route = "login"
}
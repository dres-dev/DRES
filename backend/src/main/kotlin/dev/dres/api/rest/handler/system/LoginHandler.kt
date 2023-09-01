package dev.dres.api.rest.handler.system

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.RestApi
import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.handler.RestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.admin.Password
import dev.dres.mgmt.admin.UserManager
import dev.dres.run.audit.AuditLogSource
import dev.dres.run.audit.AuditLogger
import dev.dres.utilities.extensions.getOrCreateSessionToken
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [PostRestHandler] that handles user-requests to login.
 *
 * @version 1.0.0
 * @author Luca Rossetto
 */
class LoginHandler() : RestHandler, PostRestHandler<ApiUser> {

    override val route = "login"
    override val apiVersion = RestApi.LATEST_API_VERSION

    data class LoginRequest(var username: String, var password: String)

    @OpenApi(
        summary = "Sets roles for session based on user account and returns a session cookie.",
        path = "/api/v2/login",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.POST],
        tags = ["User"],
        requestBody = OpenApiRequestBody([OpenApiContent(LoginRequest::class)]),
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiUser::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): ApiUser {

        val loginRequest = try {
            ctx.bodyAsClass(LoginRequest::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error.", ctx)
        }


        /* Validate login request. */
        val username = loginRequest.username
        val password = Password.Plain(loginRequest.password)
        val user = UserManager.getMatchingApiUser(username, password) ?: throw ErrorStatusException(
            401,
            "Invalid credentials. Please try again!",
            ctx
        )

        val sessionToken = ctx.getOrCreateSessionToken()

        AccessManager.registerUserForSession(sessionToken, user)
        AuditLogger.login(loginRequest.username, AuditLogSource.REST, sessionToken)

        //explicitly set cookie on login
        ctx.cookie(AccessManager.SESSION_COOKIE_NAME, sessionToken, AccessManager.SESSION_COOKIE_LIFETIME)

        user.sessionId = sessionToken
        return user

    }

}

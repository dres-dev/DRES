package dev.dres.api.rest.handler

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.dbo.DAO
import dev.dres.data.model.admin.PlainPassword
import dev.dres.data.model.admin.UserName
import dev.dres.mgmt.admin.UserManager
import dev.dres.mgmt.admin.UserManager.getMatchingUser
import dev.dres.run.audit.AuditLogEntry
import dev.dres.run.audit.AuditLogger
import dev.dres.run.audit.LogEventSource
import dev.dres.utilities.extensions.sessionId
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*

class LoginHandler(private val audit: DAO<AuditLogEntry>) : RestHandler, PostRestHandler<UserDetails> {

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

        val username = UserName(loginRequest.username)
        val password = PlainPassword(loginRequest.password)

        val user = getMatchingUser(username, password)
                ?: throw ErrorStatusException(401, "Invalid credentials. Please try again!", ctx)

        AccessManager.setUserForSession(ctx.sessionId(), user)
        AuditLogger.login(loginRequest.username, ctx.sessionId(), LogEventSource.REST)

        return UserDetails.create(UserManager.get(username)!!, ctx)

    }

    override val route = "login"
}
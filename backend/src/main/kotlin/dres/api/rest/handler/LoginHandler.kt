package dres.api.rest.handler

import dres.api.rest.AccessManager
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.types.status.SuccessStatus
import dres.data.dbo.DAO
import dres.data.model.admin.PlainPassword
import dres.data.model.admin.UserName
import dres.mgmt.admin.UserManager.getMatchingUser
import dres.run.audit.AuditLogEntry
import dres.run.audit.AuditLogger
import dres.run.audit.LogEventSource
import dres.utilities.extensions.sessionId
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*

class LoginHandler(private val audit: DAO<AuditLogEntry>) : RestHandler, PostRestHandler<SuccessStatus> {


    data class LoginRequest(var username: String, var password: String)

    @OpenApi(summary = "Sets roles for session based on user account and returns a session cookie.", path = "/api/login",
            method = HttpMethod.POST,
            tags = ["User"],
            requestBody = OpenApiRequestBody([OpenApiContent(LoginRequest::class)]),
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ])
    override fun doPost(ctx: Context) : SuccessStatus{

        val loginRequest = try {
            ctx.bodyAsClass(LoginRequest::class.java)
        }catch (e: BadRequestResponse){
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error.")
        }

        val username = UserName(loginRequest.username)
        val password = PlainPassword(loginRequest.password)

        val user = getMatchingUser(username, password)
                ?: throw ErrorStatusException(401, "Invalid credentials. Please try again!")

        AccessManager.setUserForSession(ctx.sessionId(), user)
        AuditLogger.login(loginRequest.username, ctx.sessionId(), LogEventSource.REST)
        return SuccessStatus("Login of '${user.username}' successful!")

    }

    override val route = "login";
}
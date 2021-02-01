package dev.dres.api.rest.handler

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.dbo.DAO
import dev.dres.run.audit.AuditLogEntry
import dev.dres.run.audit.AuditLogger
import dev.dres.run.audit.LogEventSource
import dev.dres.utilities.extensions.sessionId
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse

class LogoutHandler(private val audit: DAO<AuditLogEntry>) : RestHandler, GetRestHandler<SuccessStatus> {

    @OpenApi(summary = "Clears all user roles of the current session.", path = "/api/logout",
            tags = ["User"],
            queryParams = [
                OpenApiParam("session", String::class, "Session Token", required = true, allowEmptyValue = false)
            ],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)])
            ])
    override fun doGet(ctx: Context): SuccessStatus {
        AccessManager.clearUserSession(ctx.sessionId())
        AuditLogger.logout(ctx.sessionId(), LogEventSource.REST)
        return SuccessStatus("Logged out")

    }

    override val route = "logout"
}
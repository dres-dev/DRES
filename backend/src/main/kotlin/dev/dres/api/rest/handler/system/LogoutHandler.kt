package dev.dres.api.rest.handler.system

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.RestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.audit.DbAuditLogSource
import dev.dres.run.audit.AuditLogger
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [GetRestHandler] that handles user-requests to logout.
 *
 * @version 2.0.0
 * @author Luca Rossetto
 */
class LogoutHandler(private val store: TransientEntityStore) : RestHandler, GetRestHandler<SuccessStatus> {
    override val apiVersion = "v2"
    
    @OpenApi(summary = "Clears all user roles of the current session.",
        path = "/api/v2/logout",
        tags = ["User"],
        queryParams = [
            OpenApiParam("session", String::class, "Session Token")
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): SuccessStatus {
        val username = AccessManager.userIdForSession(ctx.sessionToken()) ?: throw ErrorStatusException(400, "You are currently not logged in.", ctx)
        val userId = AccessManager.userIdForSession(ctx.sessionToken()) ?: throw ErrorStatusException(400, "You are currently not logged in.", ctx)
        return store.transactional {
            AuditLogger.logout(userId, DbAuditLogSource.REST, ctx.sessionToken()!!)
            AccessManager.deregisterUserSession(ctx.sessionToken()!!)

            SuccessStatus("User '${username}' logged out successfully.")
        }
    }
    override val route = "logout"
}
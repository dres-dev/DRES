package dev.dres.api.rest.handler.users

import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.DeleteRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.admin.DbUser
import dev.dres.mgmt.admin.UserManager
import dev.dres.run.RunExecutor
import dev.dres.run.audit.AuditLogger
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * An [AbstractUserHandler] to delete [DbUser]s.
 *
 * @author Loris Sauter
 * @version 2.0.0
 */
class DeleteUsersHandler() : AbstractUserHandler(), DeleteRestHandler<ApiUser>, AccessManagedRestHandler {
    override val permittedRoles = setOf(ApiRole.ADMIN)

    /** [DeleteUsersHandler] requires [ApiRole.ADMIN]. */
    override val route = "user/{userId}"

    @OpenApi(
        summary = "Deletes the specified user. Requires ADMIN privileges",
        path = "/api/v2/user/{userId}", methods = [HttpMethod.DELETE],
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [OpenApiParam("userId", String::class, "User ID", required = true)],
        tags = ["User"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiUser::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)], description = "If the user could not be found"),
            OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doDelete(ctx: Context): ApiUser {
        val user = userFromContext(ctx)
        val caller = userFromSession(ctx)

        /*
         * Deleting yourself logs you out, and if you are the last admin nobody can restore the
         * installation. Deleting a user who is currently judging or participating also invalidates a
         * running evaluation, which has no way to replace a member mid-run.
         */
        if (user.id == caller.id) {
            throw ErrorStatusException(400, "You cannot delete your own account.", ctx)
        }
        if (user.role == ApiRole.ADMIN && UserManager.list().count { it.role == ApiRole.ADMIN } <= 1) {
            throw ErrorStatusException(400, "The last remaining administrator cannot be deleted.", ctx)
        }
        val attachedRuns = user.id?.let { id -> RunExecutor.managers().filter { it.template.hasParticipant(id) } } ?: emptyList()
        if (attachedRuns.isNotEmpty()) {
            throw ErrorStatusException(
                400,
                "User (${user.id}) is a participant of running evaluation(s) ${attachedRuns.joinToString { it.id }}.",
                ctx
            )
        }

        if (UserManager.delete(id = user.id)) {
            AuditLogger.userDeleted(user.id!!, caller.id, ctx.sessionToken())
            return user
        } else {
            throw ErrorStatusException(500, "Could not delete the user (${user.id})", ctx)
        }
    }
}

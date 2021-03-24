package dev.dres.data.model.run

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.UID
import dev.dres.data.model.admin.Role
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.competition.TeamId
import dev.dres.run.RunManager
import dev.dres.utilities.extensions.sessionId
import io.javalin.http.Context

/**
 * The [RunActionContext] captures and encapsulates information usually required during the interaction with a [RunManager].
 * It exposes information available to the OpenAPI facility (e.g., through session management) to the [RunManager].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.0
 */
data class RunActionContext(val userId: UserId, val teamId: TeamId?, val roles: Set<Role>) {

    /** True if the user associated with this [RunActionContext] acts as [Role.ADMIN]*/
    val isAdmin: Boolean
        get() = this.roles.contains(Role.ADMIN)

    companion object {
        /** A static [RunActionContext] used for internal invocations by DRES. Always acts as an implicit [Role.ADMIN]. */
        val INTERNAL = RunActionContext(UID.EMPTY, UID.EMPTY, setOf(Role.ADMIN))

        /**
         * Constructs a [RunActionContext] from a [Context] and a [RunManager].
         *
         * @param ctx The Javalin [Context] to construct the [RunActionContext] from.
         * @param runManager The [RunManager] to construct the [RunActionContext] for.
         */
        fun runActionContext(ctx: Context, runManager: RunManager) : RunActionContext {
            val userId = AccessManager.getUserIdForSession(ctx.sessionId()) ?: throw ErrorStatusException(403, "Unauthorized user.", ctx)
            val roles = AccessManager.rolesOfSession(ctx.sessionId()).map { Role.fromRestRole(it) }.toSet()
            val teamId = runManager.description.teams.find { it.users.contains(userId) }?.uid
            return RunActionContext(userId, teamId, roles)
        }
    }
}

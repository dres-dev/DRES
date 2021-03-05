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

data class RunActionContext(val userId: UserId, val teamId: TeamId?, val roles: Set<Role>) {
    val isAdmin: Boolean
    get() = roles.contains(Role.ADMIN)

    companion object {
        fun runActionContext(ctx: Context, runManager: RunManager) : RunActionContext {
            val userId = AccessManager.getUserIdForSession(ctx.sessionId()) ?: throw ErrorStatusException(403, "Unauthorized user", ctx)
            val roles = AccessManager.rolesOfSession(ctx.sessionId()).map { Role.fromRestRole(it) }.toSet()
            val teamId = runManager.competitionDescription.teams.find { it.users.contains(userId) }?.uid

            return RunActionContext(userId, teamId, roles)
        }

        val DUMMY_ADMIN = RunActionContext(UID.EMPTY, UID.EMPTY, setOf(Role.ADMIN))
    }
}

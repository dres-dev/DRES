package dres.api.rest

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.core.security.Role;


object AccessManager {

    fun manage(handler: Handler, ctx: Context, permittedRoles: Set<Role>) {
        when {
            permittedRoles.isEmpty() -> handler.handle(ctx) //fallback in case no roles are set, none are required
            permittedRoles.contains(RestApiRole.ANYONE) -> handler.handle(ctx)
            rolesOfSession(ctx.req.session.id).any { it in permittedRoles } -> handler.handle(ctx)
            else -> ctx.status(401).json("Unauthorized")
        }
    }

    private val sessionRoleMap = mutableMapOf<String, MutableSet<Role>>()

    private fun rolesOfSession(sessionId: String): Set<Role> = sessionRoleMap[sessionId] ?: emptySet()

    fun addRoleToSession(sessionId: String, vararg roles: Role) {
        if (sessionRoleMap.containsKey(sessionId)){
            sessionRoleMap[sessionId]!!.addAll(roles)
        } else {
            sessionRoleMap[sessionId] = mutableSetOf(*roles)
        }
    }

    fun clearRoles(sessionId: String) {
        sessionRoleMap[sessionId]?.clear()
    }

}

enum class RestApiRole : Role { ANYONE, VIEWER, USER, ADMIN }
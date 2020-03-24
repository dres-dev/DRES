package dres.api.rest

import dres.data.model.admin.User
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.http.Handler


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
    private val sessionUserMap = mutableMapOf<String, Long>()

    fun setUserforSession(sessionId: String, user: User){

        if (!sessionRoleMap.containsKey(sessionId)){
            sessionRoleMap[sessionId] = mutableSetOf()
        }

        sessionRoleMap[sessionId]!!.addAll(
                when(user.role) {
                    dres.data.model.admin.Role.ADMIN -> arrayOf(RestApiRole.VIEWER, RestApiRole.JUDGE, RestApiRole.ADMIN)
                    dres.data.model.admin.Role.JUDGE -> arrayOf(RestApiRole.VIEWER, RestApiRole.JUDGE)
                    dres.data.model.admin.Role.VIEWER -> arrayOf(RestApiRole.VIEWER)
                }
        )

        sessionUserMap[sessionId] = user.id

    }

    fun clearUserSession(sessionId: String){
        sessionRoleMap.remove(sessionId)
        sessionUserMap.remove(sessionId)
    }

    private fun rolesOfSession(sessionId: String): Set<Role> = sessionRoleMap[sessionId] ?: emptySet()

    fun getUserIdforSession(sessionId: String): Long? = sessionUserMap[sessionId]


}

enum class RestApiRole : Role { ANYONE, VIEWER, JUDGE, ADMIN }
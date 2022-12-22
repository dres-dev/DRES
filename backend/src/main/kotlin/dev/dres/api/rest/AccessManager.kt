package dev.dres.api.rest

import dev.dres.data.model.UID
import dev.dres.data.model.admin.User
import dev.dres.run.RunManager
import dev.dres.utilities.extensions.sessionToken
import io.javalin.security.RouteRole
import io.javalin.http.Context
import io.javalin.http.Handler
import java.util.concurrent.ConcurrentHashMap


object AccessManager {

    const val SESSION_COOKIE_NAME = "SESSIONID"
    const val SESSION_COOKIE_LIFETIME = 60 * 60 * 24 //a day

    val SESSION_TOKEN_CHAR_POOL : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9') + '-' + '_'
    const val SESSION_TOKEN_LENGTH = 32

    fun manage(handler: Handler, ctx: Context, permittedRoles: Set<RouteRole>) {
        when {
            permittedRoles.isEmpty() -> handler.handle(ctx) //fallback in case no roles are set, none are required
            permittedRoles.contains(RestApiRole.ANYONE) -> handler.handle(ctx)
            rolesOfSession(ctx.sessionToken()).any { it in permittedRoles } -> handler.handle(ctx)
            else -> ctx.status(401)
        }
    }

    private val sessionRoleMap = ConcurrentHashMap<String, MutableSet<RestApiRole>>()
    private val sessionUserMap = ConcurrentHashMap<String, UID>()

    /** Map keeping track of all [RunManager]s a specific user is eligible for. */
    private val usersToRunMap = ConcurrentHashMap<UID,MutableSet<RunManager>>()

    val currentSessions: Set<String>
        get() = sessionUserMap.keys

    fun setUserForSession(sessionId: String, user: User){

        if (!sessionRoleMap.containsKey(sessionId)){
            sessionRoleMap[sessionId] = mutableSetOf()
        }

        sessionRoleMap[sessionId]!!.addAll(
                when(user.role) {
                    dev.dres.data.model.admin.Role.ADMIN -> arrayOf(RestApiRole.VIEWER, RestApiRole.PARTICIPANT, RestApiRole.JUDGE, RestApiRole.ADMIN)
                    dev.dres.data.model.admin.Role.JUDGE -> arrayOf(RestApiRole.VIEWER, RestApiRole.JUDGE)
                    dev.dres.data.model.admin.Role.PARTICIPANT -> arrayOf(RestApiRole.VIEWER, RestApiRole.PARTICIPANT)
                    dev.dres.data.model.admin.Role.VIEWER -> arrayOf(RestApiRole.VIEWER)
                }
        )

        sessionUserMap[sessionId] = user.id

    }

    fun clearUserSession(sessionId: String?){
        if (sessionId == null) {
            return
        }
        sessionRoleMap.remove(sessionId)
        sessionUserMap.remove(sessionId)
    }

    fun rolesOfSession(sessionId: String?): Set<RestApiRole> = if(sessionId == null) emptySet() else sessionRoleMap[sessionId] ?: emptySet()

    fun getUserIdForSession(sessionId: String?): UID? = if (sessionId == null) null else sessionUserMap[sessionId]

    /**
     * Registers a [RunManager] for quick lookup of user ID to eligible [RunManager].
     *
     * @param runManager The [RunManager] to register.
     */
    fun registerRunManager(runManager: RunManager)  {
        runManager.description.teams.flatMap { t -> t.users }.forEach {
            if (this.usersToRunMap.containsKey(it)) {
                this.usersToRunMap[it]?.add(runManager)
            } else {
                this.usersToRunMap.put(it, mutableSetOf(runManager))
            }
        }

    }

    /**
     * Registers a [RunManager] for quick lookup of user ID to eligible [RunManager].
     *
     * @param runManager The [RunManager] to deregister.
     */
    fun deregisterRunManager(runManager: RunManager) {
        /* Remove the RunManager. */
        val idsToDrop = mutableSetOf<UID>()
        for ((k,v) in this.usersToRunMap) {
            if (v.contains(runManager)) {
                v.remove(runManager)
                if (v.isEmpty()) {
                    idsToDrop.add(k)
                }
            }
        }

        /* Cleanup the map. */
        idsToDrop.forEach { this.usersToRunMap.remove(it) }
    }

    /**
     * Returns all registered [RunManager]s for the given [userId].
     *
     * @param userId The ID of the [User] to return [RunManager]s for.
     */
    fun getRunManagerForUser(userId: UID): Set<RunManager> {
        return this.usersToRunMap[userId] ?: emptySet()
    }
}

enum class RestApiRole : RouteRole { ANYONE, VIEWER, PARTICIPANT, JUDGE, ADMIN }
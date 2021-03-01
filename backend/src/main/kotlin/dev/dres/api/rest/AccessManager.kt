package dev.dres.api.rest

import dev.dres.data.model.UID
import dev.dres.data.model.admin.User
import dev.dres.run.RunManager
import dev.dres.utilities.extensions.sessionId
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.http.Handler
import java.util.concurrent.ConcurrentHashMap


object AccessManager {

    fun manage(handler: Handler, ctx: Context, permittedRoles: Set<Role>) {
        when {
            permittedRoles.isEmpty() -> handler.handle(ctx) //fallback in case no roles are set, none are required
            permittedRoles.contains(RestApiRole.ANYONE) -> handler.handle(ctx)
            rolesOfSession(ctx.sessionId()).any { it in permittedRoles } -> handler.handle(ctx)
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

    fun clearUserSession(sessionId: String){
        sessionRoleMap.remove(sessionId)
        sessionUserMap.remove(sessionId)
    }

    fun rolesOfSession(sessionId: String): Set<RestApiRole> = sessionRoleMap[sessionId] ?: emptySet()

    fun getUserIdForSession(sessionId: String): UID? = sessionUserMap[sessionId]

    /**
     * Registers a [RunManager] for quick lookup of user ID to eligible [RunManager].
     *
     * @param runManager The [RunManager] to register.
     */
    fun registerRunManager(runManager: RunManager)  {
        runManager.competitionDescription.teams.flatMap { t -> t.users }.forEach {
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

enum class RestApiRole : Role { ANYONE, VIEWER, PARTICIPANT, JUDGE, ADMIN }
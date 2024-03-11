package dev.dres.api.rest

import dev.dres.api.rest.handler.users.SessionToken
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.admin.UserId
import dev.dres.run.RunManager
import dev.dres.utilities.extensions.sessionToken
import io.javalin.security.RouteRole
import io.javalin.http.Context
import io.javalin.http.Handler
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.flatMapDistinct
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.HashMap
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 *
 */
object AccessManager {

    const val SESSION_COOKIE_NAME = "SESSIONID"
    const val SESSION_COOKIE_LIFETIME = 60 * 60 * 24 //a day

    val SESSION_TOKEN_CHAR_POOL : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9') + '-' + '_'
    const val SESSION_TOKEN_LENGTH = 32

    fun manage(handler: Handler, ctx: Context, permittedRoles: Set<RouteRole>) {
        when {
            permittedRoles.isEmpty() -> handler.handle(ctx) //fallback in case no roles are set, none are required
            permittedRoles.contains(ApiRole.ANYONE) -> handler.handle(ctx)
            rolesOfSession(ctx.sessionToken()).any { it in permittedRoles } -> handler.handle(ctx)
            else -> ctx.status(401)
        }
    }

    fun hasAccess(ctx: Context): Boolean {
        val permittedRoles = ctx.routeRoles()
        return when {
            permittedRoles.isEmpty() -> true
            permittedRoles.contains(ApiRole.ANYONE) -> true
            rolesOfSession(ctx.sessionToken()).any { it in permittedRoles } -> true
            else -> false
        }
    }

    /** An internal [ConcurrentHashMap] that maps [SessionToken]s to [ApiRole]s. */
    private val sessionRoleMap = HashMap<SessionToken, MutableSet<ApiRole>>()

    /** An internal [ConcurrentHashMap] that maps [SessionToken]s to [UserId]s. */
    private val sessionUserMap = HashMap<SessionToken, UserId>()

    /** Map keeping track of all [RunManager]s a specific user is eligible for. */
    private val usersToRunMap = HashMap<UserId,MutableSet<RunManager>>()

    /** A [Set] of all [SessionToken]s. */
    val currentSessions: Set<SessionToken>
        get() = this.locks.read { Collections.unmodifiableSet(this.sessionUserMap.keys) }

    /** A [ReentrantReadWriteLock] that mediates access to maps. */
    private val locks = ReentrantReadWriteLock()

    /**
     * Registers a [DbUser] for a given [SessionToken]. Usually happens upon login.
     *
     * @param sessionId The [SessionToken] to register the [DbUser] for.
     * @param user The [DbUser] to register.
     */
    fun registerUserForSession(sessionId: String, user: ApiUser) = this.locks.write {
        require(user.id != null) { "Can only register user with userId for session. This is a programmer's error!"}
        if (!this.sessionRoleMap.containsKey(sessionId)){
            this.sessionRoleMap[sessionId] = mutableSetOf()
        }
        this.sessionUserMap[sessionId] = user.id
        this.sessionRoleMap[sessionId]!!.addAll(
            when(user.role) {
                ApiRole.ADMIN -> arrayOf(ApiRole.VIEWER, ApiRole.PARTICIPANT, ApiRole.JUDGE, ApiRole.ADMIN)
                ApiRole.JUDGE -> arrayOf(ApiRole.VIEWER, ApiRole.JUDGE)
                ApiRole.PARTICIPANT -> arrayOf(ApiRole.VIEWER, ApiRole.PARTICIPANT)
                ApiRole.VIEWER -> arrayOf(ApiRole.VIEWER)
                else -> throw IllegalStateException("Role held by user is unknown!")
            }
        )
    }

    /**
     * Deregisters a [DbUser] for a given [SessionToken]. Usually happens upon logout.
     *
     * @param sessionId The [SessionToken] to register the [DbUser] for.
     */
    fun deregisterUserSession(sessionId: String) = this.locks.write {
        this.sessionRoleMap.remove(sessionId)
        this.sessionUserMap.remove(sessionId)
    }

    /**
     * Queries and returns the [UserId] for the given [SessionToken].
     *
     * @param sessionId The [SessionToken] to query.
     * @return [UserId] or null if no [DbUser] is logged in.
     */
    fun userIdForSession(sessionId: String?): UserId? = this.locks.read {
        this.sessionUserMap[sessionId]
    }



    /**
     * Queries and returns the [ApiRole]s for the given [SessionToken].
     *
     * @param sessionId The [SessionToken] to query.
     * @return Set of [ApiRole] or empty set if no user is logged in.
     */
    fun rolesOfSession(sessionId: String?): Set<ApiRole> = this.locks.read {
        this.sessionRoleMap[sessionId] ?: emptySet()
    }


    /**
     * Registers a [RunManager] for quick lookup of user ID to eligible [RunManager].
     *
     * @param runManager The [RunManager] to register.
     */
    fun registerRunManager(runManager: RunManager) = this.locks.write {
        runManager.template.teams.flatMap { it.users }.forEach {
            if (this.usersToRunMap.containsKey(it.id)) {
                this.usersToRunMap[it.id]?.add(runManager)
            } else {
                this.usersToRunMap[it.id!!] = mutableSetOf(runManager)
            }
        }
    }

    /**
     * Registers a [RunManager] for quick lookup of user ID to eligible [RunManager].
     *
     * @param runManager The [RunManager] to deregister.
     */
    fun deregisterRunManager(runManager: RunManager) = this.locks.write {
        /* Remove the RunManager. */
        val idsToDrop = mutableSetOf<UserId>()
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
     * @param userId The [UserId] of the [DbUser] to return [RunManager]s for.
     */
    fun getRunManagerForUser(userId: UserId): Set<RunManager> = this.locks.read {
        return this.usersToRunMap[userId] ?: emptySet()
    }
}

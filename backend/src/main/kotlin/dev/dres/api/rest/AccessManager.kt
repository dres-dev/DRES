package dev.dres.api.rest

import dev.dres.api.rest.handler.users.SessionId
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.admin.Role
import dev.dres.data.model.admin.User
import dev.dres.data.model.admin.UserId
import dev.dres.run.RunManager
import dev.dres.utilities.extensions.sessionId
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

    /** An internal [ConcurrentHashMap] that maps [SessionId]s to [ApiRole]s. */
    private val sessionRoleMap = HashMap<SessionId, MutableSet<ApiRole>>()

    /** An internal [ConcurrentHashMap] that maps [SessionId]s to [UserId]s. */
    private val sessionUserMap = HashMap<SessionId, UserId>()

    /** Map keeping track of all [RunManager]s a specific user is eligible for. */
    private val usersToRunMap = HashMap<UserId,MutableSet<RunManager>>()

    /** A [Set] of all [SessionId]s. */
    val currentSessions: Set<SessionId>
        get() = this.locks.read { Collections.unmodifiableSet(this.sessionUserMap.keys) }

    /** A [ReentrantReadWriteLock] that mediates access to maps. */
    private val locks = ReentrantReadWriteLock()

    /**
     * Helper function that manages access for requests through the Javalin RESTful API.
     *
     * @param handler
     * @param ctx
     * @param permittedRoles
     */
    fun manage(handler: Handler, ctx: Context, permittedRoles: Set<RouteRole>) = this.locks.read {
        when {
            permittedRoles.isEmpty() -> handler.handle(ctx) //fallback in case no roles are set, none are required
            permittedRoles.contains(ApiRole.ANYONE) -> handler.handle(ctx)
            rolesOfSession(ctx.sessionId()).any { it in permittedRoles } -> handler.handle(ctx)
            else -> ctx.status(401)
        }
    }

    /**
     * Registers a [User] for a given [SessionId]. Usually happens upon login.
     *
     * @param sessionId The [SessionId] to register the [User] for.
     * @param user The [User] to register.
     */
    fun registerUserForSession(sessionId: String, user: User) = this.locks.write {
        if (!this.sessionRoleMap.containsKey(sessionId)){
            this.sessionRoleMap[sessionId] = mutableSetOf()
        }

        this.sessionUserMap[sessionId] = user.id
        this.sessionRoleMap[sessionId]!!.addAll(
            when(user.role) {
                Role.ADMIN -> arrayOf(ApiRole.VIEWER, ApiRole.PARTICIPANT, ApiRole.JUDGE, ApiRole.ADMIN)
                Role.JUDGE -> arrayOf(ApiRole.VIEWER, ApiRole.JUDGE)
                Role.PARTICIPANT -> arrayOf(ApiRole.VIEWER, ApiRole.PARTICIPANT)
                Role.VIEWER -> arrayOf(ApiRole.VIEWER)
                else -> throw IllegalStateException("Role held by user is unknown!")
            }
        )
    }

    /**
     * Deregisters a [User] for a given [SessionId]. Usually happens upon logout.
     *
     * @param sessionId The [SessionId] to register the [User] for.
     */
    fun deregisterUserSession(sessionId: String) = this.locks.write {
        this.sessionRoleMap.remove(sessionId)
        this.sessionUserMap.remove(sessionId)
    }

    /**
     * Queries and returns the [UserId] for the given [SessionId].
     *
     * @param sessionId The [SessionId] to query.
     * @return [UserId] or null if no [User] is logged in.
     */
    fun userIdForSession(sessionId: String): UserId? = this.locks.read {
        this.sessionUserMap[sessionId]
    }

    /**
     * Queries and returns the [ApiRole]s for the given [SessionId].
     *
     * @param sessionId The [SessionId] to query.
     * @return Set of [ApiRole] or empty set if no user is logged in.
     */
    fun rolesOfSession(sessionId: String): Set<ApiRole> = this.locks.read {
        this.sessionRoleMap[sessionId] ?: emptySet()
    }


    /**
     * Registers a [RunManager] for quick lookup of user ID to eligible [RunManager].
     *
     * @param runManager The [RunManager] to register.
     */
    fun registerRunManager(runManager: RunManager) = this.locks.write {
        runManager.description.teams.flatMapDistinct { it.users }.asSequence().forEach {
            if (this.usersToRunMap.containsKey(it.id)) {
                this.usersToRunMap[it.id]?.add(runManager)
            } else {
                this.usersToRunMap[it.id] = mutableSetOf(runManager)
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
     * @param userId The [UserId] of the [User] to return [RunManager]s for.
     */
    fun getRunManagerForUser(userId: UserId): Set<RunManager> = this.locks.read {
        return this.usersToRunMap[userId] ?: emptySet()
    }
}
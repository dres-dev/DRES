package dev.dres.api.rest

import dev.dres.api.rest.handler.users.SessionToken
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.run.ApiRunProperties
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

    /**
     * Decides whether the session of [ctx] may invoke a route that requires [permittedRoles].
     *
     * This is the single implementation of the role-gate rule; [hasAccess] is a thin wrapper so that
     * the live request pipeline and any other caller cannot drift apart.
     *
     * @param permittedRoles The [RouteRole]s the route accepts; an empty set means "no roles required".
     * @param session The session token of the caller, or null when there is none.
     * @return true if the request may proceed.
     */
    fun isPermitted(permittedRoles: Set<RouteRole>, session: SessionToken?): Boolean = when {
        permittedRoles.isEmpty() -> true //fallback in case no roles are set, none are required
        permittedRoles.contains(ApiRole.ANYONE) -> true
        else -> rolesOfSession(session).any { it in permittedRoles }
    }

    /**
     * Decides whether the caller of [ctx] may invoke the route it matched.
     *
     * The outcome is normally enforced by `routes.beforeMatched`. Note that this hook does not run for
     * WebSocket upgrades, which is why [dev.dres.api.rest.WsUpgradeGate] exists.
     */
    fun hasAccess(ctx: Context): Boolean = isPermitted(ctx.routeRoles(), ctx.sessionToken())

    /**
     * Decides whether the session of [ctx] may observe the state of [manager]'s run.
     *
     * A run may be configured so that its participants do not see what the run shows - typically a
     * retention study where participants must not watch the other teams. The rule is about the
     * *participant* relation to the run, so it is applied to non-admins only, and it is stated once here
     * rather than repeated in every viewer handler, where it had drifted: each copy tested
     * `ctx.isParticipant()`, which excludes judges, and none of them agreed on a message or a status code.
     *
     * @param ctx The request [Context], used for the session's roles.
     * @param runProperties The run's viewer policy.
     * @return True when the session may observe the run's state.
     */
    fun canObserveRunState(ctx: Context, runProperties: ApiRunProperties): Boolean =
        canObserveRunState(ctx.sessionToken(), runProperties)

    /**
     * The same decision as [canObserveRunState] for a bare session, so it can be tested without a request.
     *
     * @param session The caller's session token, or null when unauthenticated.
     * @param runProperties The run's viewer policy.
     * @return True when the session may observe the run's state.
     */
    fun canObserveRunState(session: SessionToken?, runProperties: ApiRunProperties): Boolean {
        val roles = this.rolesOfSession(session)
        if (roles.contains(ApiRole.ADMIN)) {
            return true
        }
        return runProperties.participantCanView || !roles.contains(ApiRole.PARTICIPANT)
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

    /**
     * The roles [AccessManager] is able to register for a session.
     *
     * [ApiRole.ANYONE] is deliberately absent: it means "no authentication required" and describes a
     * request, not a user, so a user holding it could never be granted a session.
     */
    val assignableRoles: Set<ApiRole> = setOf(ApiRole.VIEWER, ApiRole.PARTICIPANT, ApiRole.JUDGE, ApiRole.ADMIN)

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
        val roles = when(user.role) {
            ApiRole.ADMIN -> setOf(ApiRole.VIEWER, ApiRole.PARTICIPANT, ApiRole.JUDGE, ApiRole.ADMIN)
            ApiRole.JUDGE -> setOf(ApiRole.VIEWER, ApiRole.JUDGE)
            ApiRole.PARTICIPANT -> setOf(ApiRole.VIEWER, ApiRole.PARTICIPANT)
            ApiRole.VIEWER -> setOf(ApiRole.VIEWER)
            /* Unreachable for users created through the API or the CLI importer, both of which reject ANYONE. */
            else -> throw IllegalStateException("Role held by user is unknown!")
        }
        this.sessionUserMap[sessionId] = user.id
        this.sessionRoleMap[sessionId] = roles.toMutableSet()
    }

    /**
     * Deregisters a [DbUser] for a given [SessionToken]. Usually happens upon logout.
     *
     * @param sessionId The [SessionToken] to register the [DbUser] for.
     */
    fun deregisterUserSession(sessionId: String) = this.locks.write {
        this.sessionRoleMap.remove(sessionId)
        val userId = this.sessionUserMap.remove(sessionId)
        /*
         * A logged-out user must not keep its run eligibility: `usersToRunMap` is otherwise only cleaned
         * when a run itself ends ([deregisterRunManager]), so a stale entry kept answering "this user may
         * observe that run" for the rest of the deployment.
         */
        if (userId != null && this.sessionUserMap.values.none { it == userId }) {
            this.usersToRunMap.remove(userId)
        }
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

    /** Resolves multiple session tokens with one access-map read lock. */
    fun userIdsForSessions(sessionIds: Collection<String>): Map<String, UserId> = this.locks.read {
        sessionIds.mapNotNull { sessionId -> this.sessionUserMap[sessionId]?.let { sessionId to it } }.toMap()
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

    /**
     * Checks whether the user associated with the given [SessionToken] is allowed to observe
     * (e.g., via WebSocket) the given [runManager], mirroring the access rules applied to the REST API.
     *
     * @param sessionId The [SessionToken] to check.
     * @param runManager The [RunManager] the session wants to observe.
     * @return True if access is permitted, false otherwise.
     */
    fun canViewEvaluation(sessionId: SessionToken?, runManager: RunManager): Boolean {
        val roles = rolesOfSession(sessionId)
        if (roles.contains(ApiRole.ADMIN)) return true
        val userId = userIdForSession(sessionId) ?: return false
        return (roles.contains(ApiRole.JUDGE) && runManager.template.judges.contains(userId)) ||
            (roles.contains(ApiRole.VIEWER) && runManager.template.viewers.contains(userId)) ||
            (roles.contains(ApiRole.PARTICIPANT) && runManager.template.hasParticipant(userId))
    }
}

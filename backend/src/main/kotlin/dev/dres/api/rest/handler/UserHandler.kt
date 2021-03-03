package dev.dres.api.rest.handler

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.RestApiRole
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.dbo.DAO
import dev.dres.data.model.UID
import dev.dres.data.model.admin.Role
import dev.dres.data.model.admin.User
import dev.dres.data.model.admin.UserName
import dev.dres.mgmt.admin.UserManager
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.sessionId
import dev.dres.utilities.extensions.toSessionId
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*

data class SessionId(val sessionId: String)

data class UserRequest(val username: String, val password: String?, val role: Role?)

data class UserDetails(val id: String, val username: String, val role: Role, val sessionId: String? = null) {

    companion object {
        fun of(user: User): UserDetails = UserDetails(user.id.string, user.username.name, user.role)
        fun create(user: User, ctx: Context): UserDetails = UserDetails(user.id.string, user.username.name, user.role, ctx.sessionId())
    }
}

abstract class UserHandler : RestHandler {

    protected fun getFromSessionOrDie(ctx: Context): User {
        return UserManager.get(id = AccessManager.getUserIdForSession(ctx.sessionId())!!)
                ?: throw ErrorStatusException(404, "User could not be found!", ctx)
    }

    protected fun getIdFromPath(ctx: Context): UID {
        val id = ctx.pathParam("userId").UID()
        if (UserManager.exists(id = id)) {
            return id
        } else {
            throw ErrorStatusException(404, "User ($id) not found!", ctx)
        }
    }

    protected fun getUserFromId(ctx: Context): User {
        val id = getIdFromPath(ctx)
        return UserManager.get(id = id) ?: throw ErrorStatusException(404, "User ($id) not found!", ctx)
    }

    protected fun getCreateUserFromBody(ctx: Context): UserRequest {
        return ctx.body<UserRequest>()
    }
}


class ListUsersHandler : UserHandler(), GetRestHandler<List<UserDetails>>, AccessManagedRestHandler {

    @OpenApi(
            summary = "Lists all available users.",
            path = "/api/user/list",
            tags = ["User"],
            responses = [OpenApiResponse("200", [OpenApiContent(Array<UserDetails>::class)])]
    )
    override fun doGet(ctx: Context) = UserManager.list().map(UserDetails.Companion::of)


    override val permittedRoles = setOf(RestApiRole.ADMIN)

    override val route = "user/list"
}

class UserDetailsHandler : UserHandler(), GetRestHandler<UserDetails>, AccessManagedRestHandler {


    @OpenApi(
            summary = "Gets details of the user with the given id",
            path = "/api/user/:userId",
            pathParams = [
                OpenApiParam("userId", UID::class, "User's UID")
            ],
            tags = ["User"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(UserDetails::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)], description = "If the user could not be found"),
                OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context) = UserDetails.of(getUserFromId(ctx))

    override val permittedRoles = RestApiRole.values().toSet()

    override val route = "user/:userId"
}

class DeleteUsersHandler : UserHandler(), DeleteRestHandler<UserDetails>, AccessManagedRestHandler {

    @OpenApi(
            summary = "Deletes the specified user. Requires ADMIN privileges",
            path = "/api/user/:userId", method = HttpMethod.DELETE,
            pathParams = [OpenApiParam("userId", Long::class, "User ID")],
            tags = ["User"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(UserDetails::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)], description = "If the user could not be found"),
                OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doDelete(ctx: Context): UserDetails {
        val user = getUserFromId(ctx)
        if (UserManager.delete(id = user.id)) {
            return UserDetails.of(user)
        } else {
            throw ErrorStatusException(500, "Could not delete the user (${user.id})", ctx)
        }
    }

    override val permittedRoles = setOf(RestApiRole.ADMIN)

    override val route = "user/:userId"
}


class CreateUsersHandler : UserHandler(), PostRestHandler<UserDetails>, AccessManagedRestHandler {

    @OpenApi(
            summary = "Creates a new user, if the username is not already taken. Requires ADMIN privileges",
            path = "/api/user", method = HttpMethod.POST,
            requestBody = OpenApiRequestBody([OpenApiContent(UserRequest::class)]),
            tags = ["User"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(UserDetails::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)], description = "If the username is already taken"),
                OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): UserDetails {
        val req = getCreateUserFromBody(ctx)
        val success = UserManager.create(req)
        if (success) {
            return UserDetails.of(UserManager.get(username = UserName(req.username))!!)
        } else {
            throw ErrorStatusException(400, "The request could not be fulfilled.", ctx)
        }
    }

    override val permittedRoles = setOf(RestApiRole.ADMIN)

    override val route = "user"
}

class UpdateUsersHandler : UserHandler(), PatchRestHandler<UserDetails>, AccessManagedRestHandler {

    @OpenApi(
            summary = "Updates the specified user, if it exists. Anyone is allowed to update their data, however only ADMINs are allowed to update anyone",
            path = "/api/user/:userId", method = HttpMethod.PATCH,
            pathParams = [OpenApiParam("userId", UID::class, "User ID")],
            requestBody = OpenApiRequestBody([OpenApiContent(UserRequest::class)]),
            tags = ["User"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(UserDetails::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPatch(ctx: Context): UserDetails {
        val id = getIdFromPath(ctx) // Id was verified that it exists
        val req = getCreateUserFromBody(ctx)
        val caller = getFromSessionOrDie(ctx)
        when {
            (caller.role == Role.ADMIN) and (caller.id != id) -> {
                /* ADMIN -- Can edit anyone */
                val success = UserManager.update(id = id, user = req)
                if (success) {
                    return UserDetails.of(UserManager.get(id = id)!!)
                } else {
                    throw ErrorStatusException(500, "Could not update user!", ctx)
                }
            }
            caller.id == id -> {
                /* Self-Update*/
                val success = UserManager.update(id = id, user = req)
                if (success) {
                    return UserDetails.of(UserManager.get(id = id)!!)
                } else {
                    throw ErrorStatusException(500, "Could not update user!", ctx)
                }
            }
            else -> throw ErrorStatusException(400, "Cannot edit user ($id) as $caller!", ctx)
        }
    }

    override val permittedRoles = setOf(RestApiRole.VIEWER)

    override val route = "user/:userId"
}

class CurrentUsersHandler : UserHandler(), GetRestHandler<UserDetails>, AccessManagedRestHandler {

    @OpenApi(
            summary = "Get information about the current user.",
            path = "/api/user",
            tags = ["User"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(UserDetails::class)]),
                OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): UserDetails {
        return UserDetails.create(getFromSessionOrDie(ctx), ctx)
    }

    override val permittedRoles = setOf(RestApiRole.VIEWER)

    override val route = "user"

}

class CurrentUsersSessionIdHandler : UserHandler(), GetRestHandler<SessionId>, AccessManagedRestHandler {

    @OpenApi(
            summary = "Get current sessionId",
            path = "/api/user/session",
            tags = ["User"],
            queryParams = [
                OpenApiParam("session", String::class, "Session Token")
            ],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SessionId::class)]),
                OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): SessionId {
        return ctx.sessionId().toSessionId()
    }

    override val permittedRoles = setOf(RestApiRole.VIEWER)

    override val route = "user/session"
}

class ActiveSessionsHandler(private val users: DAO<User>) : GetRestHandler<List<UserDetails>>, AccessManagedRestHandler {

    override val permittedRoles = setOf(RestApiRole.ADMIN)
    override val route = "user/session/active/list"


    @OpenApi(
            summary = "Get details of all current user sessions",
            path = "/api/user/session/active/list",
            tags = ["User"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<UserDetails>::class)]),
                OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): List<UserDetails> {

        return AccessManager.currentSessions.map { session ->
            val userId = AccessManager.getUserIdForSession(session)
                    ?: return@map UserDetails(UID.EMPTY.string, "??", Role.VIEWER, session)
            val user = users[userId] ?: return@map UserDetails(userId.string, "??", Role.VIEWER, session)
            UserDetails(
                    user.id.string, user.username.name, user.role, session
            )
        }

    }

}

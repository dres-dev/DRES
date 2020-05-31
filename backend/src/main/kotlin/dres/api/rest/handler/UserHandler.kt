package dres.api.rest.handler

import dres.api.rest.AccessManager
import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.data.dbo.DAO
import dres.data.model.admin.Role
import dres.data.model.admin.User
import dres.data.model.admin.UserName
import dres.mgmt.admin.UserManager
import dres.utilities.extensions.sessionId
import dres.utilities.extensions.toSessionId
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*

data class SessionId(val sessionId:String)

data class UserRequest(val username: String, val password: String?, val role: Role?)

data class UserDetails(val id: Long, val username: String, val role: Role, val sessionId: String? = null) {

    companion object {
        fun of(user: User): UserDetails = UserDetails(user.id, user.username.name, user.role)
        fun create(user:User, ctx:Context): UserDetails = UserDetails(user.id,user.username.name, user.role, ctx.sessionId())
    }
}

abstract class UserHandler() : RestHandler {

    protected fun getFromSessionOrDie(ctx: Context): User {
        return UserManager.get(id = AccessManager.getUserIdForSession(ctx.sessionId())!!)
                ?: throw ErrorStatusException(404, "User could not be found!")
    }

    protected fun getIdFromPath(ctx: Context): Long {
        val id = ctx.pathParam("id").toLongOrNull()
                ?: throw ErrorStatusException(400, "Path parameter 'id' invalid formatted or non-existent!")
        if(UserManager.exists(id=id)){
            return id
        }else{
            throw ErrorStatusException(404, "User ($id) not found!")
        }
    }

    protected fun getUserFromId(ctx: Context): User {
        val id = getIdFromPath(ctx)
        return UserManager.get(id = id) ?: throw ErrorStatusException(404, "User ($id) not found!")
    }

    protected fun getCreateUserFromBody(ctx: Context): UserRequest {
        return ctx.body<UserRequest>()
    }



}


class ListUsersHandler() : UserHandler(), GetRestHandler<List<UserDetails>>, AccessManagedRestHandler {

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

class DeleteUsersHandler() : UserHandler(), DeleteRestHandler<UserDetails>, AccessManagedRestHandler {

    @OpenApi(
            summary = "Deletes the specified user. Requires ADMIN privileges",
            path = "/api/user/:id", method = HttpMethod.DELETE,
            pathParams = [OpenApiParam("id", Long::class, "User ID")],
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
            throw ErrorStatusException(500, "Could not delete the user (${user.id})")
        }
    }

    override val permittedRoles = setOf(RestApiRole.ADMIN)

    override val route = "user/:id"
}


class CreateUsersHandler() : UserHandler(), PostRestHandler<UserDetails>, AccessManagedRestHandler {

    @OpenApi(
            summary = "Creates a new user, if the username is not already taken. Requires ADMIN privileges",
            path = "/api/user/create", method = HttpMethod.POST,
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
        if(success){
            return UserDetails.of(UserManager.get(username = UserName(req.username))!!)
        }else{
            throw ErrorStatusException(400, "The request could not be fulfilled.")
        }
    }

    override val permittedRoles = setOf(RestApiRole.ADMIN)

    override val route = "user/create"
}

class UpdateUsersHandler() : UserHandler(), PatchRestHandler<UserDetails>, AccessManagedRestHandler {

    @OpenApi(
            summary = "Updates the specified user, if it exists. Anyone is allowed to update their data, however only ADMINs are allowed to update anyone",
            path = "/api/user/:id", method = HttpMethod.PATCH,
            pathParams = [OpenApiParam("id", Long::class, "User ID")],
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
        when{
            (caller.role == Role.ADMIN) and (caller.id != id)-> {
                /* ADMIN -- Can edit anyone */
                val success = UserManager.update(id=id, user=req)
                if(success){
                    return UserDetails.of(UserManager.get(id=id)!!)
                }else{
                    throw ErrorStatusException(500, "Could not update user!")
                }
            }
            caller.id == id -> {
                /* Self-Update*/
                val success = UserManager.update(id=id, user=req)
                if(success){
                    return UserDetails.of(UserManager.get(id=id)!!)
                }else{
                    throw ErrorStatusException(500, "Could not update user!")
                }
            }
            else -> throw ErrorStatusException(400, "Cannot edit user ($id) as $caller!")
        }
    }

    override val permittedRoles = setOf(RestApiRole.VIEWER)

    override val route = "user/:id"
}

class CurrentUsersHandler() : UserHandler(), GetRestHandler<UserDetails>, AccessManagedRestHandler {

    @OpenApi(
            summary = "Get information about the current user.",
            path = "/api/user/info",
            tags = ["User"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(UserDetails::class)]),
                OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context) : UserDetails {
        return UserDetails.create(getFromSessionOrDie(ctx),ctx)
    }

    override val permittedRoles = setOf(RestApiRole.VIEWER)

    override val route = "user/info"

}

class CurrentUsersSessionIdHandler(): UserHandler(), GetRestHandler<SessionId>, AccessManagedRestHandler {

    @OpenApi(
            summary = "Get current sessionId",
            path = "/api/user/session",
            tags = ["User"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SessionId::class)]),
                OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context) : SessionId {
        return ctx.sessionId().toSessionId()
    }

    override val permittedRoles = setOf(RestApiRole.VIEWER)

    override val route = "user/session"
}

class ActiveSessionsHandler(private val users : DAO<User>) : GetRestHandler<List<UserDetails>>, AccessManagedRestHandler {

    override val permittedRoles = setOf(RestApiRole.ADMIN)
    override val route = "user/allCurrentSessions"


    @OpenApi(
            summary = "Get details of all current user sessions",
            path = "/api/user/allCurrentSessions",
            tags = ["User"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<UserDetails>::class)]),
                OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): List<UserDetails> {

        return AccessManager.currentSessions.map {session ->
            val userId = AccessManager.getUserIdForSession(session) ?: return@map UserDetails(-1, "??", Role.VIEWER, session)
            val user = users[userId] ?: return@map UserDetails(userId, "??", Role.VIEWER, session)
            UserDetails(
                    user.id, user.username.name, user.role, session
            )
        }

    }

}

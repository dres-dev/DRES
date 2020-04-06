package dres.api.rest.handler

import dres.api.rest.AccessManager
import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.data.model.admin.Role
import dres.data.model.admin.User
import dres.data.model.admin.UserName
import dres.mgmt.admin.UserManager
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*


abstract class UserHandler() : RestHandler {

    data class UserDetails(val id: Long, val username: String, val role: Role) {

        companion object {
            fun of(user: User): UserDetails = UserDetails(user.id, user.username.name, user.role)
        }
    }

    protected fun getFromSessionOrDie(ctx: Context): User {
        return UserManager.get(id = AccessManager.getUserIdforSession(ctx.req.session.id)!!)
                ?: throw ErrorStatusException(404, "User could not be found!")
    }

    protected fun getIdFromPath(ctx: Context): Long {
        return ctx.pathParam("id").toLongOrNull()
                ?: throw ErrorStatusException(400, "Path parameter 'id' invalid formatted or non-existent!")
    }

    protected fun getUserFromId(ctx: Context): User {
        val id = getIdFromPath(ctx)
        return UserManager.get(id = id) ?: throw ErrorStatusException(404, "User ($id) not found!")
    }

    protected fun getCreateUserFromBody(ctx: Context): CreateUserRequest {
        return ctx.body<CreateUserRequest>()
    }

    data class CreateUserRequest(val username: String, val password: String, val role: Role)

}


class ListUsersHandler() : UserHandler(), GetRestHandler<List<UserHandler.UserDetails>>, AccessManagedRestHandler {

    @OpenApi(
            summary = "Lists all availabe users.",
            path = "/api/user/list",
            tags = ["User"],
            responses = [OpenApiResponse("200", [OpenApiContent(Array<UserDetails>::class)])]
    )
    override fun doGet(ctx: Context) = UserManager.list().map(UserDetails.Companion::of)


    override val permittedRoles = setOf(RestApiRole.ADMIN)

    override val route = "user/list"
}

class DeleteUsersHandler() : UserHandler(), DeleteRestHandler<UserHandler.UserDetails>, AccessManagedRestHandler {

    @OpenApi(
            summary = "Delete the user, if allowed to do so",
            path = "/api/user/:id",
            pathParams = [OpenApiParam(name = "id", description = "The id of the user to delete")],
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


class CreateUsersHandler() : UserHandler(), PostRestHandler<UserHandler.UserDetails>, AccessManagedRestHandler {


    @OpenApi(
            summary = "Creates a new user, if the username is not already taken",
            path = "/api/user/create",
            requestBody = OpenApiRequestBody(content = [OpenApiContent(CreateUserRequest::class)]),
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
            throw ErrorStatusException(400, "Username ${req.username} already taken")
        }
    }

    override val permittedRoles = setOf(RestApiRole.ADMIN)

    override val route = "user/create"
}

class CurrentUsersHandler() : UserHandler(), GetRestHandler<UserHandler.UserDetails>, AccessManagedRestHandler {

    @OpenApi(
            summary = "Get information about the current user.",
            path = "/api/user/info",
            tags = ["User"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(UserDetails::class)]),
                OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): UserDetails {
        return UserDetails.of(getFromSessionOrDie(ctx))
    }

    override val permittedRoles = setOf(RestApiRole.VIEWER)

    override val route = "user/info"

}
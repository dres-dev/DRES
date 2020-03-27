package dres.api.rest.handler

import dres.api.rest.AccessManager
import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.data.dbo.DAO
import dres.data.model.admin.Role
import dres.data.model.admin.User
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiResponse


abstract class UserHandler(protected val users: DAO<User>) : RestHandler {

    data class UserDetails(val id: Long, val username: String, val role: Role) {

        companion object {
            fun of(user: User): UserDetails = UserDetails(user.id, user.username.name, user.role)
        }
    }
}

class ListUsersHandler(users: DAO<User>) : UserHandler(users), GetRestHandler, AccessManagedRestHandler {

    @OpenApi(
            summary = "Lists all availabe users.",
            path = "/api/user/list",
            tags = ["User"],
            responses = [OpenApiResponse("200", [OpenApiContent(Array<UserDetails>::class)])]
    )
    override fun get(ctx: Context) {
        ctx.json(users.map(UserDetails.Companion::of))
    }

    override val permittedRoles = setOf(RestApiRole.ADMIN)

    override val route = "user/list"

}

class CurrentUsersHandler(users: DAO<User>) : UserHandler(users), GetRestHandler, AccessManagedRestHandler {

    @OpenApi(
            summary = "Get information about the current user.",
            path = "/api/user/info",
            tags = ["User"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(UserDetails::class)]),
                OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun get(ctx: Context) {
        val user = this.users[AccessManager.getUserIdforSession(ctx.req.session.id)!!]
        if (user != null) {
            ctx.json(UserDetails.of(user))
        } else {
            ctx.status(500).json(ErrorStatus("User could not be found!"))
        }
    }

    override val permittedRoles = setOf(RestApiRole.VIEWER)

    override val route = "user/info"

}
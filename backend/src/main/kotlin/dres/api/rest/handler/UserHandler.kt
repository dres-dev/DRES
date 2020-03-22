package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.data.dbo.DAO
import dres.data.model.admin.Role
import dres.data.model.admin.User
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiResponse


abstract class UserHandler(protected val users: DAO<User>) : RestHandler {

    data class UserDetails(val username: String, val role: Role) {

        companion object {
            fun of(user: User): UserDetails = UserDetails(user.username.name, user.role)
        }

    }

}

class ListUsersHandler(users: DAO<User>) : UserHandler(users), GetRestHandler, AccessManagedRestHandler {

    @OpenApi(
            summary = "List all users",
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
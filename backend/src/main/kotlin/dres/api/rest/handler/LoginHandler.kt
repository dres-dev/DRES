package dres.api.rest.handler

import dres.api.rest.AccessManager
import dres.api.rest.RestApiRole
import dres.data.dbo.DAO
import dres.data.model.admin.PlainPassword
import dres.data.model.admin.Role
import dres.data.model.admin.User
import dres.data.model.admin.UserName
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiFormParam
import org.mindrot.jbcrypt.BCrypt

class LoginHandler(private val dao: DAO<User>) : RestHandler, PostRestHandler {



    @OpenApi(summary = "Sets roles for session based on user account", path = "/api/log", formParams = arrayOf(
            OpenApiFormParam(name = "user", type = String::class), OpenApiFormParam(name = "pass", type = String::class)
    ))
    override fun post(ctx: Context) {
        val parameters = ctx.formParamMap()


        if (!parameters.containsKey("user") || !parameters.containsKey("pass")) {
            ctx.status(400).json("parameters required")
            return
        }

        val username = UserName(parameters["user"]!!.first())
        val password = PlainPassword(parameters["pass"]!!.first())

        val user = getMatchingUser(dao, username, password)

        if(user != null){
            val userRoles = when(user.role) {
                Role.ADMIN -> arrayOf(RestApiRole.VIEWER, RestApiRole.JUDGE, RestApiRole.ADMIN)
                Role.JUDGE -> arrayOf(RestApiRole.VIEWER, RestApiRole.JUDGE)
                Role.VIEWER -> arrayOf(RestApiRole.VIEWER)
            }
            AccessManager.addRoleToSession(ctx.req.session.id, *userRoles)
            ctx.json("login successful")
        } else {
            ctx.status(401).json("invalid credentials")
        }

    }

    private fun getMatchingUser(dao: DAO<User>, username: UserName, password: PlainPassword) : User?  {
        val user = dao.find { it.username == username } ?: return null
        return if (user.password.check(password)) user else null
    }

    override val route = "login";
}
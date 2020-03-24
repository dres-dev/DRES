package dres.api.rest.handler

import dres.api.rest.AccessManager
import dres.api.rest.RestApiRole
import dres.data.dbo.DAO
import dres.data.model.admin.PlainPassword
import dres.data.model.admin.Role
import dres.data.model.admin.User
import dres.data.model.admin.UserName
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*

class LoginHandler(private val dao: DAO<User>) : RestHandler, PostRestHandler {


    data class LoginRequest(var username: String, var password: String)

    @OpenApi(summary = "Sets roles for session based on user account", path = "/api/login", method = HttpMethod.POST,
    requestBody = OpenApiRequestBody([OpenApiContent(LoginRequest::class)]),
    responses = [OpenApiResponse("200"), OpenApiResponse("401")])
    override fun post(ctx: Context) {

        var loginRequest = try {
            ctx.bodyAsClass(LoginRequest::class.java)
        }catch (e: BadRequestResponse){
            ctx.status(400).json("invalid request parameters")
            return
        }


        val username = UserName(loginRequest.username)
        val password = PlainPassword(loginRequest.password)

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
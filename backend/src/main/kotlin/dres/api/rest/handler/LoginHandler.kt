package dres.api.rest.handler

import dres.api.rest.AccessManager
import dres.api.rest.RestApiRole
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi

class LoginHandler : RestHandler, GetRestHandler {

    //TODO basic testing code, replace with something meaningful
    @OpenApi(summary = "Sets roles for session based on user account", path = "/api/log")
    override fun get(ctx: Context) {
        val parameters = ctx.req.parameterMap


        if (!parameters.containsKey("user") || !parameters.containsKey("pass")) {
            ctx.status(400).json("parameters required")
            return
        }

        val username = parameters["user"]!!.first()
        val password = parameters["pass"]!!.first()

        if(username == "admin" && password == "secure"){
            AccessManager.addRoleToSession(ctx.req.session.id, RestApiRole.VIEWER, RestApiRole.USER, RestApiRole.ADMIN)
            ctx.json("login successful")
        } else {
            ctx.status(401).json("invalid credentials")
        }

    }

    override val route = "login";
}
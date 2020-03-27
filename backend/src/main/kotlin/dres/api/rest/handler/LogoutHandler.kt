package dres.api.rest.handler

import dres.api.rest.AccessManager
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi

class LogoutHandler : RestHandler, GetRestHandler {

    @OpenApi(summary = "Clears all user roles of the current session", path = "/api/logout")
    override fun get(ctx: Context) {

        AccessManager.clearUserSession(ctx.req.session.id);
        ctx.json("logged out")

    }

    override val route = "logout";
}
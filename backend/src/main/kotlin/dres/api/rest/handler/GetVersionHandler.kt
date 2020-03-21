package dres.api.rest.handler

import io.javalin.http.Context

class GetVersionHandler : GetRestHandler {
    override fun get(ctx: Context) {
        ctx.result("0.1")
    }

    override val route: String = "version"


}
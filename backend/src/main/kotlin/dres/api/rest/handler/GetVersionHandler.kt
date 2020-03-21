package dres.api.rest.handler

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi

class GetVersionHandler : GetRestHandler {

    @OpenApi(summary = "returns the API version", path = "/api/version")
    override fun get(ctx: Context) {
        ctx.result("0.1")
    }

    override val route: String = "version"


}
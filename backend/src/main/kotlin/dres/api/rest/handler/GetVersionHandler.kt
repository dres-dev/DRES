package dres.api.rest.handler

import dres.api.rest.types.status.SuccessStatus
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi

class GetVersionHandler : GetRestHandler<SuccessStatus> {

    @OpenApi(summary = "Returns the API version", path = "/api/version")
    override fun doGet(ctx: Context) = SuccessStatus("0.1")

    override val route: String = "version"


}
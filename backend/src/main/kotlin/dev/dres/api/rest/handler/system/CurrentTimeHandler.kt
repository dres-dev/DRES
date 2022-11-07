package dev.dres.api.rest.handler.system

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.system.CurrentTime
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse

/**
 * A [GetRestHandler] that returns the current server time.
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
class CurrentTimeHandler : GetRestHandler<CurrentTime> {

    override val route = "status/time"
    override val apiVersion = "v1"

    @OpenApi(summary = "Returns the current time on the server.",
        path = "/api/v1/status/time",
        methods = [HttpMethod.GET],
        tags = ["Status"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(CurrentTime::class)])
        ])
    override fun doGet(ctx: Context): CurrentTime = CurrentTime()
}
package dev.dres.api.rest.handler

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.HttpMethod
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiResponse

data class CurrentTime(val timeStamp: Long = System.currentTimeMillis())

class CurrentTimeHandler : GetRestHandler<CurrentTime> {

    override val route = "status/time"

    @OpenApi(summary = "Returns the current time on the server.",
        path = "/api/status/time",
        method = HttpMethod.GET,
        tags = ["Status"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(CurrentTime::class)])
        ])
    override fun doGet(ctx: Context): CurrentTime = CurrentTime()

}
package dev.dres.api.rest.handler.log

import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.log.QueryEventLog
import dev.dres.run.RunManager
import dev.dres.run.eventstream.EventStreamProcessor
import dev.dres.run.eventstream.InvalidRequestEvent
import dev.dres.run.eventstream.QueryEventLogEvent
import dev.dres.utilities.extensions.activeManagerForUser
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.bodyAsClass
import io.javalin.openapi.*

/**
 * A client facing [PostRestHandler] that can be used to sumit query logs.
 *
 * @author Loris Sauter
 * @author Luca Rossetto
 * @version 2.0.0
 */
class QueryLogHandler : AbstractLogHandler() {
    override val route = "log/query/{evaluationId}"

    @OpenApi(summary = "Accepts query logs from participants for the specified evaluation.",
            path = "/api/v2/log/query/{evaluationId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
            methods = [HttpMethod.POST],
            requestBody = OpenApiRequestBody([OpenApiContent(QueryEventLog::class)]),
            tags = ["Log"],
            pathParams = [
                OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false)
            ],
            queryParams = [
                OpenApiParam("session", String::class, "Session Token", required = true, allowEmptyValue = false)
            ],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val evaluationManager = ctx.eligibleManagerForId<RunManager>()
        // val evaluationManager = ctx.activeManagerForUser()
        val queryEventLog = try {
            ctx.bodyAsClass<QueryEventLog>()
        } catch (e: BadRequestResponse){
            EventStreamProcessor.event(InvalidRequestEvent(ctx.sessionToken(), evaluationManager.id, ctx.body()))
            throw ErrorStatusException(400, "Invalid parameters: ${e.localizedMessage}", ctx)
        }.copy(serverTimeStamp = System.currentTimeMillis())
        EventStreamProcessor.event(QueryEventLogEvent(ctx.sessionToken(), evaluationManager.id, queryEventLog))
        return SuccessStatus("Log received")
    }
}


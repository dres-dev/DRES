package dres.api.rest.handler

import dres.api.rest.AccessManager
import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.types.status.SuccessStatus
import dres.data.model.UID
import dres.data.model.log.QueryEventLog
import dres.data.model.log.QueryResultLog
import dres.run.RunManager
import dres.run.RunManagerStatus
import dres.run.eventstream.EventStreamProcessor
import dres.run.eventstream.InvalidRequestEvent
import dres.run.eventstream.QueryEventLogEvent
import dres.run.eventstream.QueryResultLogEvent
import dres.utilities.extensions.sessionId
import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*

abstract class LogHandler : PostRestHandler<SuccessStatus>, AccessManagedRestHandler {

    private fun getRelevantManagers(userId: UID): Set<RunManager> = AccessManager.getRunManagerForUser(userId)

    protected fun getActiveRun(userId: UID, ctx: Context): RunManager {
        val managers = getRelevantManagers(userId).filter { it.status != RunManagerStatus.CREATED && it.status != RunManagerStatus.TERMINATED }
        if (managers.isEmpty()) {
            throw ErrorStatusException(404, "There is currently no eligible competition with an active task.", ctx)
        }

        if (managers.size > 1) {
            throw ErrorStatusException(409, "More than one possible competition found: ${managers.joinToString { it.competitionDescription.name }}", ctx)
        }

        return managers.first()
    }

}

class QueryLogHandler : LogHandler() {
    override val permittedRoles: Set<Role> = setOf(RestApiRole.ADMIN, RestApiRole.PARTICIPANT)
    override val route = "log/query"

    @OpenApi(summary = "Accepts query logs from participants",
            path = "/log/query",
            method = HttpMethod.POST,
            requestBody = OpenApiRequestBody([OpenApiContent(QueryEventLog::class)]),
            tags = ["Log"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])]
    )
    override fun doPost(ctx: Context): SuccessStatus {

        val userId = AccessManager.getUserIdForSession(ctx.sessionId()) ?: throw ErrorStatusException(401, "Authorization required.", ctx)
        val run = getActiveRun(userId, ctx)


        val queryEventLog = try {
            ctx.body<QueryEventLog>()
        } catch (e: BadRequestResponse){
            EventStreamProcessor.event(InvalidRequestEvent(ctx.sessionId(), run.id, ctx.body()))
            throw ErrorStatusException(400, "Invalid parameters: ${e.localizedMessage}", ctx)
        }.copy(serverTimeStamp = System.currentTimeMillis())

        EventStreamProcessor.event(QueryEventLogEvent(ctx.sessionId(), run.id, queryEventLog))

        return SuccessStatus("Log received")
    }

}

class ResultLogHandler : LogHandler() {
    override val permittedRoles: Set<Role> = setOf(RestApiRole.ADMIN, RestApiRole.PARTICIPANT)
    override val route = "log/result"

    @OpenApi(summary = "Accepts result logs from participants",
            path = "/log/result",
            method = HttpMethod.POST,
            requestBody = OpenApiRequestBody([OpenApiContent(QueryResultLog::class)]),
            tags = ["Log"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])]
    )
    override fun doPost(ctx: Context): SuccessStatus {

        val userId = AccessManager.getUserIdForSession(ctx.sessionId()) ?: throw ErrorStatusException(401, "Authorization required.", ctx)
        val run = getActiveRun(userId, ctx)

        val queryResultLog = try {
            ctx.body<QueryResultLog>()
        } catch (e: BadRequestResponse){
            EventStreamProcessor.event(InvalidRequestEvent(ctx.sessionId(), run.id, ctx.body()))
            throw ErrorStatusException(400, "Invalid parameters: ${e.localizedMessage}", ctx)
        }.copy(serverTimeStamp = System.currentTimeMillis())

        EventStreamProcessor.event(QueryResultLogEvent(ctx.sessionId(), run.id, queryResultLog))

        return SuccessStatus("Log received")
    }

}
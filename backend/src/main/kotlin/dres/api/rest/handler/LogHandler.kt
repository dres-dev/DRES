package dres.api.rest.handler

import dres.api.rest.AccessManager
import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.types.status.SuccessStatus
import dres.data.model.log.QueryEventLog
import dres.data.model.log.QueryResultLog
import dres.run.RunManager
import dres.run.RunManagerStatus
import dres.utilities.extensions.sessionId
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*

abstract class LogHandler : PostRestHandler<SuccessStatus>, AccessManagedRestHandler {

    private fun getRelevantManagers(userId: Long): Set<RunManager> = AccessManager.getRunManagerForUser(userId)

    protected fun getActiveRun(userId: Long): RunManager {
        val managers = getRelevantManagers(userId).filter { it.status == RunManagerStatus.RUNNING_TASK }
        if (managers.isEmpty()) {
            throw ErrorStatusException(404, "There is currently no eligible competition with an active task.")
        }

        if (managers.size > 1) {
            throw ErrorStatusException(409, "More than one possible competition found: ${managers.joinToString { it.competitionDescription.name }}")
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

        val userId = AccessManager.getUserIdForSession(ctx.sessionId()) ?: throw ErrorStatusException(401, "Authorization required.")
        val run = getActiveRun(userId)


        val queryLog = ctx.body<QueryEventLog>()



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

        val userId = AccessManager.getUserIdForSession(ctx.sessionId()) ?: throw ErrorStatusException(401, "Authorization required.")
        val run = getActiveRun(userId)

        val queryLog = ctx.body<QueryResultLog>()

        //TODO validate and store

        return SuccessStatus("Log received")
    }

}
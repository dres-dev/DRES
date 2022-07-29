package dev.dres.api.rest.handler

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.RestApiRole
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.UID
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.run.*
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.sessionId
import io.javalin.core.security.RouteRole
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse

abstract class AbstractCompetitionRunClientInfoHandler : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<RouteRole> = setOf(RestApiRole.VIEWER)
    override val apiVersion = "v1"

    private fun userId(ctx: Context): UID = AccessManager.getUserIdForSession(ctx.sessionId())!!

    fun isParticipant(ctx: Context): Boolean = AccessManager.rolesOfSession(ctx.sessionId()).contains(RestApiRole.PARTICIPANT) && !AccessManager.rolesOfSession(ctx.sessionId()).contains(
        RestApiRole.ADMIN)

    fun getRelevantManagers(ctx: Context): List<RunManager> {
        val userId = userId(ctx)
        return RunExecutor.managers().filter { m -> m.description.teams.any { it.users.contains(userId) } }
    }

    fun getRun(ctx: Context, runId: UID): RunManager? {
        val userId = userId(ctx)
        val run = RunExecutor.managerForId(runId) ?: return null
        if (run.description.teams.any { it.users.contains(userId) }) {
            return run
        }
        return null

    }

    fun runId(ctx: Context) = ctx.pathParamMap().getOrElse("runId") {
        throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx)
    }.UID()
}

data class ClientRunInfo(
    val id: String,
    val name: String,
    val description: String?,
    val status: RunManagerStatus
) {
    constructor(runManager: RunManager) : this(
        runManager.id.string,
        runManager.name,
        runManager.description.description,
        runManager.status
    )
}

data class ClientRunInfoList(val runs : List<ClientRunInfo>)

class ListCompetitionRunClientInfoHandler : AbstractCompetitionRunClientInfoHandler(), GetRestHandler<ClientRunInfoList> {

    override val route = "client/run/info/list"

    @OpenApi(
        summary = "Lists an overview of all competition runs visible to the current client",
        path = "/api/v1/client/run/info/list",
        tags = ["Client Run Info"],
        queryParams = [
            OpenApiParam("session", String::class, "Session Token", required = true, allowEmptyValue = false)
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ClientRunInfoList::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): ClientRunInfoList =
        ClientRunInfoList(getRelevantManagers(ctx).map { ClientRunInfo(it) })

}

data class ClientTaskInfo(
    val id: String,
    val name: String,
    val taskGroup: String,
    val remainingTime: Long,
    val running: Boolean
)

class CompetitionRunClientCurrentTaskInfoHandler : AbstractCompetitionRunClientInfoHandler(), GetRestHandler<ClientTaskInfo> {

    override val route = "client/run/info/currentTask/{runId}"

    @OpenApi(
        summary = "Returns an overview of the currently active task for a run",
        path = "/api/v1/client/run/info/currentTask/{runId}",
        tags = ["Client Run Info"],
        queryParams = [
            OpenApiParam("session", String::class, "Session Token", required = true, allowEmptyValue = false)
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ClientTaskInfo::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): ClientTaskInfo {

        val run = getRun(ctx, runId(ctx)) ?: throw ErrorStatusException(404, "Specified run not found", ctx)
        val rac = runActionContext(ctx, run)

        if (run !is InteractiveRunManager) {
            throw ErrorStatusException(404, "Specified run is not interactive", ctx)
        }

        val task = run.currentTask(rac) ?: throw ErrorStatusException(404, "Specified run has no active task", ctx)


        return ClientTaskInfo(
            task.uid.string,
            task.description.name,
            task.description.taskGroup.name,
            when(run.status){
                RunManagerStatus.CREATED -> 0
                RunManagerStatus.ACTIVE -> {
                    when(task.status) {
                        TaskRunStatus.CREATED,
                        TaskRunStatus.PREPARING -> task.duration
                        TaskRunStatus.RUNNING ->run.timeLeft(rac) / 1000
                        TaskRunStatus.ENDED -> 0
                    }
                }
                RunManagerStatus.TERMINATED -> 0
            },
            task.isRunning
        )

    }

}

package dres.api.rest.handler

import dres.api.rest.AccessManager
import dres.api.rest.RestApiRole
import dres.api.rest.types.run.RunType
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.types.status.SuccessStatus
import dres.data.dbo.DAO
import dres.data.model.competition.Competition
import dres.data.model.run.CompetitionRun
import dres.run.RunExecutor
import dres.run.RunManager
import dres.run.SynchronousRunManager
import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*


abstract class AbstractCompetitionRunAdminRestHandler : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.ADMIN)

    private fun userId(ctx: Context): Long = AccessManager.getUserIdforSession(ctx.req.session.id)!!

    private fun isAdmin(ctx: Context): Boolean = AccessManager.rolesOfSession(ctx.req.session.id).contains(RestApiRole.ADMIN)

    fun getRelevantManagers(ctx: Context): List<RunManager> {
        if (isAdmin(ctx)){
            return RunExecutor.managers()
        }
        val userId = userId(ctx)
        return RunExecutor.managers().filter { it.competition.teams.any { it.users.contains(userId) } }
    }

    fun getRun(ctx: Context, runId: Long): RunManager? {
        if (isAdmin(ctx)){
            return RunExecutor.managerForId(runId)
        }
        val userId = userId(ctx)
        val run = RunExecutor.managerForId(runId) ?: return null
        if (run.competition.teams.any { it.users.contains(userId) }){
            return run
        }
        return null
    }

    fun runId(ctx: Context) = ctx.pathParamMap().getOrElse("runId") {
        throw ErrorStatusException(404, "Parameter 'runId' is missing!'")
    }.toLong()
}

/**
 * REST handler to create a [CompetitionRun].
 */
class CreateCompetitionRunAdminHandler(val runs: DAO<CompetitionRun>, val competitions: DAO<Competition>) : AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {

    private fun competitionById(id: Long): Competition =
            competitions[id] ?: throw ErrorStatusException(404, "Competition with ID $id not found.'")

    override val route = "run/admin/create"

    @OpenApi(
            summary = "Creates a new competition run from an existing competition",
            path = "/api/run/admin/create",
            method = HttpMethod.POST,
            requestBody = OpenApiRequestBody([OpenApiContent(CompetitionStart::class)]),
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {

        val competitionStartMessage = try {
            ctx.bodyAsClass(CompetitionStart::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!")
        }

        val competitionToStart = this.competitionById(competitionStartMessage.competitionId)

        /* Prepare... */
        try {
            val manager = when (competitionStartMessage.type) {
                RunType.ASYNCHRONOUS -> TODO()
                RunType.SYNCHRONOUS -> SynchronousRunManager(competitionToStart, competitionStartMessage.name, emptyList(), RunExecutor, this.runs)
            }

            /**... and schedule RunManager. */
            RunExecutor.schedule(manager)

            return SuccessStatus("Competition '${competitionStartMessage.name}' was started and is running with ID ${manager.runId}.")
        } catch (e: IllegalArgumentException) {
            throw ErrorStatusException(400, e.message ?: "Invalid parameters. This is a programmers error!")
        }
    }

    data class CompetitionStart(val competitionId: Long, val name: String, val type: RunType, val scoreboards: Array<String>)
}

/**
 * REST handler to start a [CompetitionRun].
 */
class StartCompetitionRunAdminHandler: AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/start"

    @OpenApi(
            summary = "Starts a competition run. This is a method for admins.",
            path = "/api/run/admin/:runId/start",
            method = HttpMethod.POST,
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found")
        try {
            run.start()
            return SuccessStatus("Run $runId was successfully started.")
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Run $runId could not be started because it is in the wrong state (state = ${run.status}).")
        }
    }
}

/**
 * REST handler to move to the next task in a [CompetitionRun].
 */
class NextTaskCompetitionRunAdminHandler: AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/task/next"

    @OpenApi(
            summary = "Moves to the next task. This is a method for admins.",
            path = "/api/run/admin/:runId/task/next",
            method = HttpMethod.POST,
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found")
        try {
            if (run.nextTask()) {
                return SuccessStatus("Task for run $runId was successfully moved to '${run.currentTask!!.name}'.")
            } else {
                throw ErrorStatusException(400, "Task for run $runId could not be changed because there are no tasks left.")
            }
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Task for run $runId could not be changed because run is in the wrong state (state = ${run.status}).")
        }
    }
}

/**
 * REST handler to move to the previous task in a [CompetitionRun].
 */
class PreviousTaskCompetitionRunAdminHandler: AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/task/previous"

    @OpenApi(
            summary = "Moves to the previous task. This is a method for admins.",
            path = "/api/run/admin/:runId/task/previous",
            method = HttpMethod.POST,
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found")
        try {
            if (run.previousTask()) {
                return SuccessStatus("Task for run $runId was successfully moved to '${run.currentTask!!.name}'.")
            } else {
                throw ErrorStatusException(400, "Task for run $runId could not be changed because there are no tasks left.")
            }
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Task for run $runId could not be changed because run is in the wrong state (state = ${run.status}).")
        }
    }
}

/**
 * REST handler to move to the previous task in a [CompetitionRun].
 */
class StartTaskCompetitionRunAdminHandler: AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/task/start"

    @OpenApi(
            summary = "Starts the current task. This is a method for admins.",
            path = "/api/run/admin/:runId/task/start",
            method = HttpMethod.POST,
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found")
        try {
            run.startTask()
            return SuccessStatus("Task '${run.currentTask!!.name}' for run $runId was successfully started.")
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Task '${run.currentTask!!.name}' for run $runId could not be started because run is in the wrong state (state = ${run.status}).")
        }
    }
}

/**
 * REST handler to terminate a [CompetitionRun].
 */
class TerminateCompetitionRunAdminHandler: AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/terminate"

    @OpenApi(
            summary = "Starts a competition run. This is a method for admins.",
            path = "/api/run/admin/:runId/terminate",
            method = HttpMethod.POST,
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(ctx, runId) ?: throw ErrorStatusException(404, "Run $runId not found")
        try {
            run.terminate()
            return SuccessStatus("Run $runId was successfully terminated.")
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Run $runId could not be terminated because it is in the wrong state (state = ${run.status}).")
        }
    }
}
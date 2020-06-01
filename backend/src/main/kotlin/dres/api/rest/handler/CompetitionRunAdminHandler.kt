package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.types.run.RunType
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.types.status.SuccessStatus
import dres.data.dbo.DAO
import dres.data.model.Config
import dres.data.model.basics.media.MediaCollection
import dres.data.model.competition.CompetitionDescription
import dres.data.model.competition.interfaces.MediaSegmentTaskDescription
import dres.data.model.run.CompetitionRun
import dres.run.RunExecutor
import dres.run.RunManager
import dres.run.RunManagerStatus
import dres.run.SynchronousRunManager
import dres.run.audit.AuditLogEntry
import dres.run.audit.AuditLogManager
import dres.run.audit.LogEventSource
import dres.run.score.scoreboard.MaxNormalizingScoreBoard
import dres.run.score.scoreboard.MeanAggregateScoreBoard
import dres.run.score.scoreboard.Scoreboard
import dres.utilities.FFmpegUtil
import dres.utilities.extensions.sessionId

import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*
import org.slf4j.LoggerFactory
import java.io.File


abstract class AbstractCompetitionRunAdminRestHandler : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.ADMIN)

    fun getRun(runId: Long): RunManager? = RunExecutor.managerForId(runId)

    fun runId(ctx: Context) = ctx.pathParamMap().getOrElse("runId") {
        throw ErrorStatusException(404, "Parameter 'runId' is missing!'")
    }.toLong()
}

/**
 * REST handler to create a [CompetitionRun].
 */
class CreateCompetitionRunAdminHandler(private val competitions: DAO<CompetitionDescription>, private val collections: DAO<MediaCollection>, config: Config) : AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {

    private val cacheLocation = File(config.cachePath + "/tasks")
    private val logger = LoggerFactory.getLogger(this.javaClass)

    private fun competitionById(id: Long): CompetitionDescription =
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

        /* ensure that only one synchronous run of a competition is happening at any given time */
        if(competitionStartMessage.type == RunType.SYNCHRONOUS && RunExecutor.managers().any {
                    it is SynchronousRunManager && it.competitionDescription == competitionToStart && it.status != RunManagerStatus.TERMINATED }
        ){
            throw ErrorStatusException(400, "Synchronous run of competition ${competitionToStart.name} already exists")
        }

        val segmentTasks = competitionToStart.tasks.filterIsInstance(MediaSegmentTaskDescription::class.java)

        /* check videos */
        segmentTasks.forEach {
            val item = it.item
            val collection = this.collections[item.collection]
                    ?: throw ErrorStatusException(400, "collection ${item.collection} not found")

            val videoFile = File(File(collection.basePath), item.location)

            if (!videoFile.exists()) {
                logger.error("file ${videoFile.absolutePath} not found for item ${item.name}")
                return@forEach
            }

            val outputFile = File(cacheLocation, it.cacheItemName())
            if(!outputFile.exists()){
                logger.warn("query video file for task ${it.name} not found, rendering to ${outputFile.absolutePath}")
                FFmpegUtil.prepareMediaSegmentTask(it, collection.basePath, cacheLocation)
            }

        }

        /* Prepare... */
        try {
            val manager = when (competitionStartMessage.type) {
                RunType.ASYNCHRONOUS -> TODO()
                RunType.SYNCHRONOUS -> SynchronousRunManager(competitionToStart, competitionStartMessage.name)
            }

            /**... and schedule RunManager. */
            RunExecutor.schedule(manager)

            return SuccessStatus("Competition '${competitionStartMessage.name}' was started and is running with ID ${manager.runId}.")
        } catch (e: IllegalArgumentException) {
            throw ErrorStatusException(400, e.message ?: "Invalid parameters. This is a programmers error!")
        }
    }

    data class CompetitionStart(val competitionId: Long, val name: String, val type: RunType, val scoreboards: Array<String>) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CompetitionStart

            if (competitionId != other.competitionId) return false
            if (name != other.name) return false
            if (type != other.type) return false
            if (!scoreboards.contentEquals(other.scoreboards)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = competitionId.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + scoreboards.contentHashCode()
            return result
        }
    }
}

/**
 * REST handler to start a [CompetitionRun].
 */
class StartCompetitionRunAdminHandler(private val audit: DAO<AuditLogEntry>): AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/start"

    @OpenApi(
            summary = "Starts a competition run. This is a method for admins.",
            path = "/api/run/admin/:runId/start",
            method = HttpMethod.POST,
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found")
        try {
            run.start()
            AuditLogManager.getAuditLogger(run.name, audit).competitionStart(LogEventSource.REST, ctx.sessionId())
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
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found")
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
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found")
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
 * REST handler to start the current task in a [CompetitionRun].
 */
class StartTaskCompetitionRunAdminHandler: AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/task/start"

    @OpenApi(
            summary = "Starts the current task. This is a method for admins.",
            path = "/api/run/admin/:runId/task/start",
            method = HttpMethod.POST,
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found")
        try {
            run.startTask()
            AuditLogManager.getAuditLogger(run.name)!!.taskStart(run.currentTask?.name ?: "no taks", LogEventSource.REST, ctx.sessionId())
            return SuccessStatus("Task '${run.currentTask!!.name}' for run $runId was successfully started.")
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Task '${run.currentTask!!.name}' for run $runId could not be started because run is in the wrong state (state = ${run.status}).")
        }
    }
}

/**
 * REST handler to abort the current task in a [CompetitionRun].
 */
class AbortTaskCompetitionRunAdminHandler: AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/task/abort"

    @OpenApi(
            summary = "Aborts the currently running task. This is a method for admins.",
            path = "/api/run/admin/:runId/task/abort",
            method = HttpMethod.POST,
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found")
        try {
            val task = run.currentTask
            run.abortTask()
            AuditLogManager.getAuditLogger(run.name)!!.taskEnd(task?.name ?: "no taks", LogEventSource.REST, ctx.sessionId())
            return SuccessStatus("Task '${run.currentTask!!.name}' for run $runId was successfully aborted.")
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Task '${run.currentTask!!.name}' for run $runId could not be aborted because run is in the wrong state (state = ${run.status}).")
        }
    }
}

/**
 * REST handler to terminate a [CompetitionRun].
 */
class TerminateCompetitionRunAdminHandler: AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/terminate"

    @OpenApi(
            summary = "Terminates a competition run. This is a method for admins.",
            path = "/api/run/admin/:runId/terminate",
            method = HttpMethod.POST,
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found")
        try {
            run.terminate()
            AuditLogManager.getAuditLogger(run.name)?.competitionEnd(LogEventSource.REST, ctx.sessionId())
            return SuccessStatus("Run $runId was successfully terminated.")
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Run $runId could not be terminated because it is in the wrong state (state = ${run.status}).")
        }
    }
}

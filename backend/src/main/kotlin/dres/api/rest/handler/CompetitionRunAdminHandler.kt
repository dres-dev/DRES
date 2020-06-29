package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.types.run.RunType
import dres.api.rest.types.run.ViewerInfo
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
import dres.run.audit.AuditLogger
import dres.run.audit.LogEventSource
import dres.utilities.FFmpegUtil
import dres.utilities.extensions.sessionId
import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.IndexOutOfBoundsException


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
class StartCompetitionRunAdminHandler: AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
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
            AuditLogger.competitionStart(run.uid, LogEventSource.REST, ctx.sessionId())
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
 * REST handler to move to the next task in a [CompetitionRun].
 */
class SwitchTaskCompetitionRunAdminHandler: AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/task/switch/:idx"

    @OpenApi(
            summary = "Moves to the specified task. This is a method for admins.",
            path = "/api/run/admin/:runId/task/switch/:idx",
            method = HttpMethod.POST,
            pathParams = [
                OpenApiParam("runId", Long::class, "Competition run ID"),
                OpenApiParam("idx", Int::class, "Index of the task to switch to.")
            ],
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
        val idx = ctx.pathParamMap().getOrElse("idx") {
            throw ErrorStatusException(404, "Parameter 'idx' is missing!'")
        }.toInt()

        try {
            run.goToTask(idx)
            return SuccessStatus("Task for run $runId was successfully moved to '${run.currentTask!!.name}'.")
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Task for run $runId could not be changed because run is in the wrong state (state = ${run.status}).")
        } catch (e: IndexOutOfBoundsException) {
            throw ErrorStatusException(404, "Task for run $runId could not be changed because index $idx is out of bounds for number of available tasks.")
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
            AuditLogger.taskStart(run.uid, run.currentTask?.name ?: "n/a", LogEventSource.REST, ctx.sessionId())
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
            AuditLogger.taskEnd(run.uid, task?.name ?: "n/a", LogEventSource.REST, ctx.sessionId())
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
            AuditLogger.competitionEnd(run.uid, LogEventSource.REST, ctx.sessionId())
            return SuccessStatus("Run $runId was successfully terminated.")
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Run $runId could not be terminated because it is in the wrong state (state = ${run.status}).")
        }
    }
}

/**
 * REST handler to adjust a [CompetitionRun]'s duration.
 */
class AdjustDurationRunAdminHandler : AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/adjust/:duration"

    @OpenApi(
            summary = "Terminates a competition run. This is a method for admins.",
            path = "/api/run/admin/:runId/adjust/:duration",
            method = HttpMethod.POST,
            pathParams = [
                OpenApiParam("runId", Long::class, "Competition Run ID"),
                OpenApiParam("duration", Int::class, "Duration to add.")
            ],
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found")
        val duration = ctx.pathParamMap().getOrElse("duration") {
            throw ErrorStatusException(404, "Parameter 'duration' is missing!'")
        }.toInt()
        try {
            run.adjustDuration(duration)
            AuditLogger.taskModified(run.uid, run.currentTask?.name ?: "n/a","Task duration adjusted by ${duration}s.", LogEventSource.REST, ctx.sessionId())
            return SuccessStatus("Duration for run $runId was successfully adjusted.")
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Duration for run $runId could not be adjusted because it is in the wrong state (state = ${run.status}).")
        } catch (e: IllegalArgumentException) {
            throw ErrorStatusException(400, "Duration for run $runId could not be adjusted because new duration would drop bellow zero (state = ${run.status}).")
        }
    }
}

/**
 * REST handler to list all viewers for a [CompetitionRun].
 */
class ListViewersRunAdminHandler : AbstractCompetitionRunAdminRestHandler(), GetRestHandler<Array<ViewerInfo>> {
    override val route: String = "run/admin/:runId/viewers"

    @OpenApi(
            summary = "Lists all registered viewers for a competition run. This is a method for admins.",
            path = "/api/run/admin/:runId/viewers",
            method = HttpMethod.GET,
            pathParams = [OpenApiParam("runId", Long::class, "Competition Run ID")],
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<ViewerInfo>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): Array<ViewerInfo> {
        val runId = runId(ctx)
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found")
        return run.viewers().map{ ViewerInfo(it.key, it.value) }.toTypedArray()
    }
}

/**
 * REST handler to force the viewer state of a viewer instance registered for a [RunManager].
 */
class ForceViewerRunAdminHandler : AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/viewers/:viewerId/force"

    @OpenApi(
            summary = "Forces a viewer with the given viewer ID into the READY state. This is a method for admins.",
            path = "/api/run/admin/:runId/viewers/:viewerId/force",
            method = HttpMethod.POST,
            pathParams = [
                OpenApiParam("runId", Long::class, "Competition Run ID"),
                OpenApiParam("viewerId", String::class, "Viewer ID")
            ],
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val viewerId = ctx.pathParamMap().getOrElse("viewerId") {
            throw ErrorStatusException(404, "Parameter 'viewerId' is missing!'")
        }
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found")
        try {
            if (run.overrideReadyState(viewerId)) {
                return SuccessStatus("State for viewer $viewerId forced successfully.")
            } else {
                throw ErrorStatusException(404, "Viewer $viewerId does not exist!'")
            }
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Viewer state for viewer $viewerId (run $runId) could not be enforced because run is in the wrong state (state = ${run.status}).")
        }
    }
}
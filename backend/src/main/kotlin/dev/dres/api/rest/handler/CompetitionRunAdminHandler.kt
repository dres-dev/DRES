package dev.dres.api.rest.handler

import dev.dres.api.rest.RestApiRole
import dev.dres.api.rest.types.competition.CompetitionStartMessage
import dev.dres.api.rest.types.run.RunType
import dev.dres.api.rest.types.run.SubmissionInfo
import dev.dres.api.rest.types.run.ViewerInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.dbo.DAO
import dev.dres.data.model.Config
import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaCollection
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.run.CompetitionRun
import dev.dres.run.RunExecutor
import dev.dres.run.RunManager
import dev.dres.run.RunManagerStatus
import dev.dres.run.SynchronousRunManager
import dev.dres.run.audit.AuditLogger
import dev.dres.run.audit.LogEventSource
import dev.dres.run.eventstream.EventStreamProcessor
import dev.dres.run.eventstream.TaskStartEvent
import dev.dres.utilities.FFmpegUtil
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.sessionId
import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*
import org.slf4j.LoggerFactory
import java.io.File


abstract class AbstractCompetitionRunAdminRestHandler : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.ADMIN)

    fun getRun(runId: UID): RunManager? = RunExecutor.managerForId(runId)

    fun runId(ctx: Context) = ctx.pathParamMap().getOrElse("runId") {
        throw ErrorStatusException(404, "Parameter 'runId' is missing!'", ctx)
    }.UID()
}

/**
 * REST handler to create a [CompetitionRun].
 */
class CreateCompetitionRunAdminHandler(private val competitions: DAO<CompetitionDescription>, private val collections: DAO<MediaCollection>, config: Config) : AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {

    private val cacheLocation = File(config.cachePath + "/tasks")
    private val logger = LoggerFactory.getLogger(this.javaClass)

    private fun competitionById(id: UID, ctx: Context): CompetitionDescription =
            competitions[id] ?: throw ErrorStatusException(404, "Competition with ID $id not found.'", ctx)

    override val route = "run/admin/create"

    @OpenApi(
            summary = "Creates a new competition run from an existing competition",
            path = "/api/run/admin/create",
            method = HttpMethod.POST,
            requestBody = OpenApiRequestBody([OpenApiContent(CompetitionStartMessage::class)]),
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {

        val competitionStartMessage = try {
            ctx.bodyAsClass(CompetitionStartMessage::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        val competitionToStart = this.competitionById(competitionStartMessage.competitionId.UID(), ctx)

        /* ensure that only one synchronous run of a competition is happening at any given time */
        if (competitionStartMessage.type == RunType.SYNCHRONOUS && RunExecutor.managers().any {
                    it is SynchronousRunManager && it.competitionDescription.id == competitionToStart.id && it.status != RunManagerStatus.TERMINATED
                }
        ) {
            throw ErrorStatusException(400, "Synchronous run of competition ${competitionToStart.name} already exists", ctx)
        }

        val segmentTasks = competitionToStart.getAllCachedVideoItems()

        /* check videos */
        segmentTasks.forEach {
            val item = it.item
            val collection = this.collections[item.collection]
                    ?: throw ErrorStatusException(400, "collection ${item.collection} not found", ctx)

            val videoFile = File(File(collection.basePath), item.location)

            if (!videoFile.exists()) {
                logger.error("file ${videoFile.absolutePath} not found for item ${item.name}")
                return@forEach
            }

            val outputFile = File(cacheLocation, it.cacheItemName())
            if (!outputFile.exists()) {
                logger.warn("query video file for item ${it.item} not found, rendering to ${outputFile.absolutePath}")
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

            return SuccessStatus("Competition '${competitionStartMessage.name}' was started and is running with ID ${manager.id}.")
        } catch (e: IllegalArgumentException) {
            throw ErrorStatusException(400, e.message ?: "Invalid parameters. This is a programmers error!", ctx)
        }
    }
}

/**
 * REST handler to start a [CompetitionRun].
 */
class StartCompetitionRunAdminHandler : AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/start"

    @OpenApi(
            summary = "Starts a competition run. This is a method for admins.",
            path = "/api/run/admin/:runId/start",
            method = HttpMethod.POST,
            pathParams = [OpenApiParam("runId", UID::class, "Competition Run ID")],
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)
        try {
            run.start()
            AuditLogger.competitionStart(run.id, LogEventSource.REST, ctx.sessionId())
            return SuccessStatus("Run $runId was successfully started.")
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Run $runId could not be started because it is in the wrong state (state = ${run.status}).", ctx)
        }
    }
}

/**
 * REST handler to move to the next task in a [CompetitionRun].
 */
class NextTaskCompetitionRunAdminHandler : AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/task/next"

    @OpenApi(
            summary = "Moves to and selects the next task. This is a method for admins.",
            path = "/api/run/admin/:runId/task/next",
            method = HttpMethod.POST,
            pathParams = [OpenApiParam("runId", UID::class, "Competition Run ID")],
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)
        try {
            if (run.nextTask()) {
                return SuccessStatus("Task for run $runId was successfully moved to '${run.currentTask!!.name}'.")
            } else {
                throw ErrorStatusException(400, "Task for run $runId could not be changed because there are no tasks left.", ctx)
            }
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Task for run $runId could not be changed because run is in the wrong state (state = ${run.status}).", ctx)
        }
    }
}

/**
 * REST handler to move to the next task in a [CompetitionRun].
 */
class SwitchTaskCompetitionRunAdminHandler : AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/task/switch/:idx"

    @OpenApi(
            summary = "Moves to and selects the specified task. This is a method for admins.",
            path = "/api/run/admin/:runId/task/switch/:idx",
            method = HttpMethod.POST,
            pathParams = [
                OpenApiParam("runId", UID::class, "Competition run ID"),
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
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)
        val idx = ctx.pathParamMap().getOrElse("idx") {
            throw ErrorStatusException(404, "Parameter 'idx' is missing!'", ctx)
        }.toInt()

        try {
            run.goToTask(idx)
            return SuccessStatus("Task for run $runId was successfully moved to '${run.currentTask!!.name}'.")
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Task for run $runId could not be changed because run is in the wrong state (state = ${run.status}).", ctx)
        } catch (e: IndexOutOfBoundsException) {
            throw ErrorStatusException(404, "Task for run $runId could not be changed because index $idx is out of bounds for number of available tasks.", ctx)
        }
    }
}

/**
 * REST handler to move to the previous task in a [CompetitionRun].
 */
class PreviousTaskCompetitionRunAdminHandler : AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/task/previous"

    @OpenApi(
            summary = "Moves to and selects the previous task. This is a method for admins.",
            path = "/api/run/admin/:runId/task/previous",
            method = HttpMethod.POST,
            pathParams = [OpenApiParam("runId", UID::class, "Competition Run ID")],
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)
        try {
            if (run.previousTask()) {
                return SuccessStatus("Task for run $runId was successfully moved to '${run.currentTask!!.name}'.")
            } else {
                throw ErrorStatusException(400, "Task for run $runId could not be changed because there are no tasks left.", ctx)
            }
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Task for run $runId could not be changed because run is in the wrong state (state = ${run.status}).", ctx)
        }
    }
}

/**
 * REST handler to start the current task in a [CompetitionRun].
 */
class StartTaskCompetitionRunAdminHandler : AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/task/start"

    @OpenApi(
            summary = "Starts the currently active task as a new task run. This is a method for admins.",
            path = "/api/run/admin/:runId/task/start",
            method = HttpMethod.POST,
            pathParams = [OpenApiParam("runId", UID::class, "Competition Run ID")],
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)
        try {
            run.startTask()
            AuditLogger.taskStart(run.id, run.currentTask?.name ?: "n/a", LogEventSource.REST, ctx.sessionId())
            EventStreamProcessor.event(TaskStartEvent(runId, run.currentTaskRun!!.uid, run.currentTask!!))
            return SuccessStatus("Task '${run.currentTask!!.name}' for run $runId was successfully started.")
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Task '${run.currentTask!!.name}' for run $runId could not be started because run is in the wrong state (state = ${run.status}).", ctx)
        }
    }
}

/**
 * REST handler to abort the current task in a [CompetitionRun].
 */
class AbortTaskCompetitionRunAdminHandler : AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/task/abort"

    @OpenApi(
            summary = "Aborts the currently running task run. This is a method for admins.",
            path = "/api/run/admin/:runId/task/abort",
            method = HttpMethod.POST,
            pathParams = [OpenApiParam("runId", UID::class, "Competition Run ID")],
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)
        try {
            val task = run.currentTask
            run.abortTask()
            AuditLogger.taskEnd(run.id, task?.name ?: "n/a", LogEventSource.REST, ctx.sessionId())
            return SuccessStatus("Task '${run.currentTask!!.name}' for run $runId was successfully aborted.")
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Task '${run.currentTask!!.name}' for run $runId could not be aborted because run is in the wrong state (state = ${run.status}).", ctx)
        }
    }
}

/**
 * REST handler to terminate a [CompetitionRun].
 */
class TerminateCompetitionRunAdminHandler : AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/terminate"

    @OpenApi(
            summary = "Terminates a competition run. This is a method for admins.",
            path = "/api/run/admin/:runId/terminate",
            method = HttpMethod.POST,
            pathParams = [OpenApiParam("runId", UID::class, "Competition Run ID")],
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = runId(ctx)
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)
        try {
            run.end()
            AuditLogger.competitionEnd(run.id, LogEventSource.REST, ctx.sessionId())
            return SuccessStatus("Run $runId was successfully terminated.")
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Run $runId could not be terminated because it is in the wrong state (state = ${run.status}).", ctx)
        }
    }
}

/**
 * REST handler to adjust a [CompetitionRun.TaskRun]'s duration.
 */
class AdjustDurationRunAdminHandler : AbstractCompetitionRunAdminRestHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "run/admin/:runId/adjust/:duration"

    @OpenApi(
            summary = "Adjusts the duration of a running task run. This is a method for admins.",
            path = "/api/run/admin/:runId/adjust/:duration",
            method = HttpMethod.POST,
            pathParams = [
                OpenApiParam("runId", UID::class, "Competition Run ID"),
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
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)
        val duration = ctx.pathParamMap().getOrElse("duration") {
            throw ErrorStatusException(404, "Parameter 'duration' is missing!'", ctx)
        }.toInt()
        try {
            run.adjustDuration(duration)
            AuditLogger.taskModified(run.id, run.currentTask?.name
                    ?: "n/a", "Task duration adjusted by ${duration}s.", LogEventSource.REST, ctx.sessionId())
            return SuccessStatus("Duration for run $runId was successfully adjusted.")
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Duration for run $runId could not be adjusted because it is in the wrong state (state = ${run.status}).", ctx)
        } catch (e: IllegalArgumentException) {
            throw ErrorStatusException(400, "Duration for run $runId could not be adjusted because new duration would drop bellow zero (state = ${run.status}).", ctx)
        }
    }
}

class ListSubmissionsPerTaskRunAdminHandler : AbstractCompetitionRunAdminRestHandler(), GetRestHandler<Array<SubmissionInfo>> {
    override val route: String = "run/admin/:runId/submissions/list/:taskId"

    @OpenApi(
            summary = "Lists all submissions for a given task and run",
            path = "/api/run/admin/:runId/submissions/list/:taskId",
            method = HttpMethod.GET,
            pathParams = [
                OpenApiParam("runId", UID::class, "Competition Run ID"),
                OpenApiParam("taskId", UID::class, "Task ID")
            ],
            tags = ["Competition Run Admin"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<SubmissionInfo>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): Array<SubmissionInfo> {
        val runId = runId(ctx)
        val run = getRun(runId) ?: throw ErrorStatusException(404, "No such run was found: $runId", ctx)

        val taskId = ctx.pathParamMap().getOrElse("taskId") {
            throw ErrorStatusException(404, "Parameter 'taskId' is missing!'", ctx)
        }.UID()
        return run.submissions.filter { it.taskRun?.taskId?.equals(taskId) ?: false }.map { SubmissionInfo.withId(it) }.toTypedArray()
    }

}

/**
 * REST handler to list all viewers for a [CompetitionRun].
 */
class ListViewersRunAdminHandler : AbstractCompetitionRunAdminRestHandler(), GetRestHandler<Array<ViewerInfo>> {
    override val route: String = "run/admin/:runId/viewer/list"

    @OpenApi(
            summary = "Lists all registered viewers for a competition run. This is a method for admins.",
            path = "/api/run/admin/:runId/viewer/list",
            method = HttpMethod.GET,
            pathParams = [OpenApiParam("runId", UID::class, "Competition Run ID")],
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
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)
        return run.viewers().map { ViewerInfo(it.key.sessionId, it.key.userName, it.key.host, it.value) }.toTypedArray()
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
                OpenApiParam("runId", UID::class, "Competition Run ID"),
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
            throw ErrorStatusException(404, "Parameter 'viewerId' is missing!'", ctx)
        }
        val run = getRun(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)
        try {
            if (run.overrideReadyState(viewerId)) {
                return SuccessStatus("State for viewer $viewerId forced successfully.")
            } else {
                throw ErrorStatusException(404, "Viewer $viewerId does not exist!'", ctx)
            }
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(400, "Viewer state for viewer $viewerId (run $runId) could not be enforced because run is in the wrong state (state = ${run.status}).", ctx)
        }
    }
}
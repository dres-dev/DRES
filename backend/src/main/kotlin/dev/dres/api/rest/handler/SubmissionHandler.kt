package dev.dres.api.rest.handler


import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.RestApiRole
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.dbo.DAO
import dev.dres.data.dbo.DaoIndexer
import dev.dres.data.model.Config
import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaCollection
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.basics.media.MediaItemSegmentList
import dev.dres.data.model.basics.media.PlayableMediaItem
import dev.dres.data.model.competition.TaskType
import dev.dres.data.model.run.*
import dev.dres.run.RunManager
import dev.dres.run.RunManagerStatus
import dev.dres.run.audit.AuditLogger
import dev.dres.run.audit.LogEventSource
import dev.dres.run.eventstream.EventStreamProcessor
import dev.dres.run.eventstream.SubmissionEvent
import dev.dres.utilities.FFmpegUtil
import dev.dres.utilities.TimeUtil
import dev.dres.utilities.extensions.sessionId
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SubmissionHandler (val collections: DAO<MediaCollection>, private val itemIndex: DaoIndexer<MediaItem, Pair<UID, String>>, private val segmentIndex: DaoIndexer<MediaItemSegmentList, UID>, private val config: Config): GetRestHandler<SuccessStatus>, AccessManagedRestHandler {
    override val permittedRoles = setOf(RestApiRole.PARTICIPANT)
    override val route = "submit"

    companion object {
        const val PARAMETER_NAME_COLLECTION = "collection"
        const val PARAMETER_NAME_ITEM = "item"
        const val PARAMETER_NAME_SHOT = "shot"
        const val PARAMETER_NAME_FRAME = "frame"
        const val PARAMETER_NAME_TIMECODE = "timecode"
    }


    private fun getRelevantManagers(userId: UID): Set<RunManager> = AccessManager.getRunManagerForUser(userId)

    private fun getActiveRun(userId: UID, ctx: Context): RunManager {
        val managers = getRelevantManagers(userId).filter { it.status == RunManagerStatus.RUNNING_TASK }
        if (managers.isEmpty()) {
            throw ErrorStatusException(404, "There is currently no eligible competition with an active task.", ctx)
        }

        if (managers.size > 1) {
            throw ErrorStatusException(409, "More than one possible competition found: ${managers.joinToString { it.competitionDescription.name }}", ctx)
        }

        return managers.first()
    }

    private fun toSubmission(ctx: Context, userId: UID, runManager: RunManager, submissionTime: Long): Submission {
        val map = ctx.queryParamMap()

        /* Find team that the user belongs to. */
        val team = runManager.competitionDescription.teams.find {
            it.users.contains(userId)
        }?.uid ?: throw ErrorStatusException(404, "No team for user '$userId' could not be found.", ctx)

        /* Find collectionId the submission belongs to.. */
        val collectionParam = map[PARAMETER_NAME_COLLECTION]?.first()
        val collectionId: UID = when {
            collectionParam != null -> this.collections.find { it.name == collectionParam }?.id
            else -> runManager.currentTask?.mediaCollectionId
        } ?: throw ErrorStatusException(404, "Media collection '$collectionParam' could not be found.", ctx)

        /* Find media item. */
        val itemParam = map[PARAMETER_NAME_ITEM]?.first() ?: throw ErrorStatusException(404, "Parameter '$PARAMETER_NAME_ITEM' is missing but required!'", ctx)
        val item = this.itemIndex[collectionId to itemParam].firstOrNull() ?:
            throw ErrorStatusException(404, "Media item '$itemParam (collection = $collectionId)' could not be found.", ctx)

        val mapToSegment = runManager.currentTask?.taskType?.options?.contains(TaskType.Options.MAP_TO_SEGMENT) == true

        return when {
            map.containsKey(PARAMETER_NAME_SHOT) && item is MediaItem.VideoItem -> {
                val segmentList = segmentIndex[item.id].firstOrNull() ?: throw ErrorStatusException(400, "Item '${item.name}' not found.", ctx)
                val time = TimeUtil.shotToTime(map[PARAMETER_NAME_SHOT]?.first()!!, item, segmentList) ?: throw ErrorStatusException(400, "Shot '${item.name}.${map[PARAMETER_NAME_SHOT]?.first()!!}' not found.", ctx)
                TemporalSubmission(team, userId, submissionTime, item, time.first, time.second)
            }
            map.containsKey(PARAMETER_NAME_FRAME) && (item is PlayableMediaItem) -> {
                val time = TimeUtil.frameToTime(map[PARAMETER_NAME_FRAME]?.first()?.toIntOrNull() ?: throw ErrorStatusException(400, "Parameter '$PARAMETER_NAME_FRAME' must be a number.", ctx), item)
                val segmentList = segmentIndex[item.id].firstOrNull() ?: throw ErrorStatusException(400, "Item '${item.name}' not found.", ctx)
                val range = if(mapToSegment && item is MediaItem.VideoItem) (TimeUtil.timeToSegment(time, item, segmentList) ?: throw ErrorStatusException(400, "No segments found for item '${item.name}'.", ctx)) else time to time
                TemporalSubmission(team, userId, submissionTime, item, range.first, range.second)
            }
            map.containsKey(PARAMETER_NAME_TIMECODE) && (item is PlayableMediaItem) -> {
                val time = TimeUtil.timeCodeToMilliseconds(map[PARAMETER_NAME_TIMECODE]?.first()!!, item) ?: throw ErrorStatusException(400, "'${map[PARAMETER_NAME_TIMECODE]?.first()!!}' is not a valid time code", ctx)
                val segmentList = segmentIndex[item.id].firstOrNull() ?: throw ErrorStatusException(400, "Item '${item.name}' not found.", ctx)
                val range = if(mapToSegment && item is MediaItem.VideoItem) (TimeUtil.timeToSegment(time, item, segmentList) ?: throw ErrorStatusException(400, "No segments found for item '${item.name}'.", ctx)) else time to time
                TemporalSubmission(team, userId, submissionTime, item, range.first, range.second)
            }
            else -> ItemSubmission(team, userId, submissionTime, item)
        }.also {
            it.taskRun = runManager.currentTaskRun
        }
    }

    @OpenApi(summary = "Endpoint to accept submissions",
            path = "/submit",
            queryParams = [
                OpenApiParam(PARAMETER_NAME_COLLECTION, String::class, "Collection identifier. Optional, in which case the default collection for the run will be considered."),
                OpenApiParam(PARAMETER_NAME_ITEM, String::class, "Identifier for the actual media object or media file."),
                OpenApiParam(PARAMETER_NAME_FRAME, Int::class, "Frame number for media with temporal progression (e.g. video)."),
                OpenApiParam(PARAMETER_NAME_SHOT, Int::class, "Shot number for media with temporal progression (e.g. video)."),
                OpenApiParam(PARAMETER_NAME_TIMECODE, String::class, "Timecode for media with temporal progression (e.g. video)."),
                OpenApiParam("session", String::class, "Session Token", required = true, allowEmptyValue = false)
            ],
            tags = ["Submission"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("208", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("409", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): SuccessStatus {
        val userId = AccessManager.getUserIdForSession(ctx.sessionId()) ?: throw ErrorStatusException(401, "Authorization required.", ctx)
        val run = getActiveRun(userId, ctx)
        val time = System.currentTimeMillis()
        val submission = toSubmission(ctx, userId, run, time)
        val result = try {
            run.postSubmission(submission)
        } catch (e: IllegalArgumentException) { //is only thrown by submission filter TODO: nicer exception type
            throw ErrorStatusException(208, "Submission rejected", ctx)
        }

        AuditLogger.submission(run.id, run.currentTask?.name ?: "no task", submission, LogEventSource.REST, ctx.sessionId(), ctx.req.remoteAddr)
        EventStreamProcessor.event(SubmissionEvent(ctx.sessionId(), run.id, run.currentTaskRun?.uid, submission))

        if (run.currentTask?.taskType?.options?.any{ it.option == TaskType.Options.HIDDEN_RESULTS} == true) { //pre-generate preview
            generatePreview(submission)
        }


        return when (result) {
            SubmissionStatus.CORRECT -> SuccessStatus("Submission correct!")
            SubmissionStatus.WRONG -> SuccessStatus("Submission incorrect! Try again")
            SubmissionStatus.INDETERMINATE -> SuccessStatus("Submission received. Waiting for verdict!")
            SubmissionStatus.UNDECIDABLE -> SuccessStatus("Submission undecidable. Try again!")
        }
    }

    private fun generatePreview(submission: Submission) {
        if (submission !is TemporalSubmissionAspect){
            return
        }
        val collection = collections[submission.item.collection] ?: return
        val cacheLocation = Paths.get(config.cachePath + "/previews")
        val cacheDir = cacheLocation.resolve("${submission.item.collection}/${submission.item.name}")
        val imgPath = cacheDir.resolve("${submission.start}.jpg")
        if (Files.exists(imgPath)){
            return
        }
        val mediaItemLocation = Path.of(collection.basePath, submission.item.location)
        FFmpegUtil.extractFrame(mediaItemLocation, submission.start, imgPath)

    }
}
package dres.api.rest.handler


import dres.api.rest.AccessManager
import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.types.status.SuccessStatus
import dres.data.dbo.DAO
import dres.data.dbo.DaoIndexer
import dres.data.model.basics.media.MediaCollection
import dres.data.model.basics.media.MediaItem
import dres.data.model.basics.media.MediaItemSegmentList
import dres.data.model.basics.media.PlayableMediaItem
import dres.data.model.competition.TaskDescriptionBase
import dres.data.model.competition.interfaces.DefinedMediaItemTaskDescription
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import dres.run.RunManager
import dres.run.RunManagerStatus
import dres.run.audit.AuditLogger
import dres.run.audit.LogEventSource
import dres.utilities.TimeUtil
import dres.utilities.extensions.sessionId
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse

class SubmissionHandler (val collections: DAO<MediaCollection>, private val itemIndex: DaoIndexer<MediaItem, Pair<Long, String>>, private val segmentIndex: DaoIndexer<MediaItemSegmentList, Long>): GetRestHandler<SuccessStatus>, AccessManagedRestHandler {
    override val permittedRoles = setOf(RestApiRole.PARTICIPANT)
    override val route = "submit"

    companion object {
        const val PARAMETER_NAME_COLLECTION = "collection"
        const val PARAMETER_NAME_ITEM = "item"
        const val PARAMETER_NAME_SHOT = "shot"
        const val PARAMETER_NAME_FRAME = "frame"
        const val PARAMETER_NAME_TIMECODE = "timecode"
    }


    private fun getRelevantManagers(userId: Long): Set<RunManager> = AccessManager.getRunManagerForUser(userId)

    private fun getActiveRun(userId: Long): RunManager {
        val managers = getRelevantManagers(userId).filter { it.status == RunManagerStatus.RUNNING_TASK }
        if (managers.isEmpty()) {
            throw ErrorStatusException(404, "There is currently no eligible competition with an active task.")
        }

        if (managers.size > 1) {
            throw ErrorStatusException(409, "More than one possible competition found: ${managers.joinToString { it.competitionDescription.name }}")
        }

        return managers.first()
    }

    private fun toSubmission(ctx: Context, userId: Long, runManager: RunManager, submissionTime: Long): Submission {
        val map = ctx.queryParamMap()
        val team = runManager.competitionDescription.teams.indexOf(runManager.competitionDescription.teams.first { it.users.contains(userId) })

        val collectionParam = map[PARAMETER_NAME_COLLECTION]?.first()
        val collectionId = when {
            collectionParam != null -> this.collections.find { it.name == collectionParam }?.id
            runManager.currentTask is TaskDescriptionBase.KisVisualTaskDescription -> {
                (runManager.currentTask as TaskDescriptionBase.KisVisualTaskDescription).item.collection
            }
            runManager.currentTask is TaskDescriptionBase.KisTextualTaskDescription -> {
                (runManager.currentTask as TaskDescriptionBase.KisTextualTaskDescription).item.collection
            }
            runManager.currentTask is TaskDescriptionBase.AvsTaskDescription -> {
                (runManager.currentTask as TaskDescriptionBase.AvsTaskDescription).defaultCollection
            }
            else -> null
        } ?: throw ErrorStatusException(404, "Media collection '$collectionParam' could not be found.")

        /* Find media item. */
        val itemParam = map[PARAMETER_NAME_ITEM]?.first() ?: throw ErrorStatusException(404, "Parameter '$PARAMETER_NAME_ITEM' is missing but required!'")
        val item = this.itemIndex[collectionId to itemParam].firstOrNull() ?:
            throw ErrorStatusException(404, "Media item '$itemParam (collection = $collectionId)' could not be found.")

        val mapToSegment = runManager.currentTask is DefinedMediaItemTaskDescription

        return when {
            map.containsKey(PARAMETER_NAME_SHOT) && item is MediaItem.VideoItem -> {
                val time = this.shotToTime(map[PARAMETER_NAME_SHOT]?.first()!!, item)
                Submission(team, userId, submissionTime, item, time.first, time.second)
            }
            map.containsKey(PARAMETER_NAME_FRAME) && (item is PlayableMediaItem) -> {
                val time = this.frameToTime(map[PARAMETER_NAME_FRAME]?.first()?.toIntOrNull() ?: throw ErrorStatusException(400, "Parameter '$PARAMETER_NAME_FRAME' must be a number."), item)
                val range = if(mapToSegment && item is MediaItem.VideoItem) timeToSegment(time, item) else time to time
                Submission(team, userId, submissionTime, item, range.first, range.second)
            }
            map.containsKey(PARAMETER_NAME_TIMECODE) && (item is PlayableMediaItem) -> {
                val time = this.timecodeToTime(map[PARAMETER_NAME_TIMECODE]?.first()!!, item)
                val range = if(mapToSegment && item is MediaItem.VideoItem) timeToSegment(time, item) else time to time
                Submission(team, userId, submissionTime, item, range.first, range.second)
            }
            else -> Submission(team, userId, submissionTime, item)
        }.also {
            it.taskRun = runManager.currentTaskRun
        }
    }

    /**
     * Converts a shot number to a timestamp in milliseconds.
     */
    private fun shotToTime(shot: String, item: MediaItem.VideoItem): Pair<Long,Long> {
        val segmentList = segmentIndex[item.id].firstOrNull() ?: throw ErrorStatusException(400, "Item '${item.name}' not found.")
        val segment = segmentList.segments.find { it.name == shot } ?: throw ErrorStatusException(400, "Shot '${item.name}.$shot' not found.")
        return TimeUtil.toMilliseconds(segment.range, item.fps)
    }

    /**
     * Converts a frame number to a timestamp in milliseconds.
     */
    private fun frameToTime(frame: Int, item: PlayableMediaItem): Long {
        return ((frame / item.fps) * 1000.0).toLong()
    }

    /**
     * Converts a timecode to a timestamp in milliseconds.
     */
    private fun timecodeToTime(timecode: String, item: PlayableMediaItem): Long {
        return TimeUtil.timeCodeToMilliseconds(timecode, item.fps) ?: throw ErrorStatusException(400, "'$timecode' is not a valid time code")
    }

    private fun timeToSegment(time: Long, item: MediaItem.VideoItem): Pair<Long,Long> {
        val segmentList = segmentIndex[item.id].firstOrNull() ?: throw ErrorStatusException(400, "Item '${item.name}' not found.")
        val segment = segmentList.segments.find {
            val range = TimeUtil.toMilliseconds(it.range, item.fps)
            range.first <= time && range.second >= time
        } ?: throw ErrorStatusException(400, "Time '$time' not in range.")
        return TimeUtil.toMilliseconds(segment.range, item.fps)
    }

    @OpenApi(summary = "Endpoint to accept submissions",
            path = "/submit",
            queryParams = [
                OpenApiParam(PARAMETER_NAME_COLLECTION, String::class, "Collection identifier. Optional, in which case the default collection for the run will be considered."),
                OpenApiParam(PARAMETER_NAME_ITEM, String::class, "Identifier for the actual media object or media file."),
                OpenApiParam(PARAMETER_NAME_FRAME, Int::class, "Frame number for media with temporal progression (e.g. video)."),
                OpenApiParam(PARAMETER_NAME_SHOT, Int::class, "Shot number for media with temporal progression (e.g. video)."),
                OpenApiParam(PARAMETER_NAME_TIMECODE, String::class, "Timecode for media with temporal progression (e.g. video).")
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
        val userId = AccessManager.getUserIdForSession(ctx.sessionId()) ?: throw ErrorStatusException(401, "Authorization required.")
        val run = getActiveRun(userId)
        val time = System.currentTimeMillis()
        val submission = toSubmission(ctx, userId, run, time)
        val result = try {
            run.postSubmission(submission)
        } catch (e: IllegalArgumentException) { //is only thrown by submission filter TODO: nicer exception type
            throw ErrorStatusException(208, "Submission rejected")
        }

        AuditLogger.submission(run.uid, run.currentTask?.name ?: "no task", submission, LogEventSource.REST, ctx.sessionId())

        return when (result) {
            SubmissionStatus.CORRECT -> SuccessStatus("Submission correct!")
            SubmissionStatus.WRONG -> SuccessStatus("Submission incorrect! Try again")
            SubmissionStatus.INDETERMINATE -> SuccessStatus("Submission received. Waiting for verdict!")
            SubmissionStatus.UNDECIDABLE -> SuccessStatus("Submission undecidable. Try again!")
        }
    }
}
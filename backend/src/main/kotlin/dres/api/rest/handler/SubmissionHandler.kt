package dres.api.rest.handler


import dres.api.rest.AccessManager
import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.types.status.SuccessStatus
import dres.data.dbo.DAO
import dres.data.model.basics.MediaCollection
import dres.data.model.basics.MediaItem
import dres.data.model.basics.MediaItemSegment
import dres.data.model.competition.TaskDescriptionBase
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import dres.run.RunManager
import dres.run.RunManagerStatus
import dres.utilities.TimeUtil

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse

class SubmissionHandler (val collections: DAO<MediaCollection>, val items: DAO<MediaItem>, val segment: DAO<MediaItemSegment>): GetRestHandler<SuccessStatus>, AccessManagedRestHandler {
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
        val member = userId

        val collectionParam = map[PARAMETER_NAME_COLLECTION]?.first()
        val collectionId = when {
            collectionParam != null -> this.collections.find { it.name == collectionParam }?.id
            runManager.currentTask is TaskDescriptionBase.KisVisualTaskDescription -> {
                (runManager.currentTask as TaskDescriptionBase.KisVisualTaskDescription).item.collection
            }
            runManager.currentTask is TaskDescriptionBase.KisTextualTaskDescription -> {
                (runManager.currentTask as TaskDescriptionBase.KisTextualTaskDescription).item.collection
            }
            else -> null
        } ?: throw ErrorStatusException(404, "Media collection '$collectionParam' could not be found.")

        /* Find media item. */
        val itemParam = map[PARAMETER_NAME_COLLECTION]?.first() ?: throw ErrorStatusException(404, "Parameter '$PARAMETER_NAME_COLLECTION' is missing but required!'")
        val item = this.items.find {
            it.name == itemParam && it.collection == collectionId
        } ?:  throw ErrorStatusException(404, "Media collection '$collectionParam.$itemParam' could not be found.")

        return when {
            map.containsKey(PARAMETER_NAME_SHOT) -> {
                val time = this.shotToTime(map[PARAMETER_NAME_FRAME]?.first()!!, item)
                Submission(team, member, submissionTime, item, time.first, time.second)
            }
            map.containsKey(PARAMETER_NAME_FRAME) -> {
                val time = this.frameToTime(map[PARAMETER_NAME_FRAME]?.first()?.toIntOrNull() ?: throw ErrorStatusException(400, "Parameter '$PARAMETER_NAME_FRAME' must be a number."), item)
                Submission(team, member, submissionTime, item, time, time)
            }
            map.containsKey(PARAMETER_NAME_TIMECODE) -> {
                val time = this.timecodeToTime(map[PARAMETER_NAME_TIMECODE]?.first()!!, item)
                Submission(team, member, submissionTime, item, time, time)
            }
            else -> Submission(team, member, submissionTime, item)
        }
    }

    /**
     * Converts a shot number to a timestamp in milliseconds.
     *
     * @param frame The []
     */
    private fun shotToTime(shot: String, item: MediaItem): Pair<Long,Long> {
        val segment = this.segment.find { it.mediaItemId == item.id && it.name == shot } ?: throw ErrorStatusException(400, "Shot '${item.name}.$shot' not found.")
        return TimeUtil.toMilliseconds(segment.range)
    }

    /**
     * Converts a frame number to a timestamp in milliseconds.
     *
     * @param frame The []
     */
    private fun frameToTime(frame: Int, item: MediaItem): Long {
        val fps = 25L /* TODO: Extract from MediaItem. */
        return (frame / fps) * 1000L
    }

    /**
     * Converts a timecode to a timestamp in milliseconds.
     *
     * @param frame The []
     */
    private fun timecodeToTime(timecode: String, item: MediaItem): Long {
        return 0L /* TODO: Make transformation. */
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
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("409", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): SuccessStatus {
        val userId = AccessManager.getUserIdforSession(ctx.req.session.id) ?: throw ErrorStatusException(401, "Authorization required.")
        val run = getActiveRun(userId)
        val time = System.currentTimeMillis()
        val result = run.postSubmission(toSubmission(ctx, userId, run, time))

        return when (result) {
            SubmissionStatus.CORRECT -> SuccessStatus("Submission correct!")
            SubmissionStatus.WRONG -> SuccessStatus("Submission correct! Try again")
            SubmissionStatus.INDETERMINATE -> SuccessStatus("Submission received. Waiting for verdict!")
            SubmissionStatus.UNDECIDABLE -> SuccessStatus("Submission undecidable. Try again!")
        }
    }
}
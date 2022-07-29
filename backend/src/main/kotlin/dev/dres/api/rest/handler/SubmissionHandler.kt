package dev.dres.api.rest.handler


import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.RestApiRole
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessfulSubmissionsStatus
import dev.dres.data.dbo.DAO
import dev.dres.data.dbo.DaoIndexer
import dev.dres.data.model.Config
import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaCollection
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.basics.media.MediaItemSegmentList
import dev.dres.data.model.basics.media.PlayableMediaItem
import dev.dres.data.model.basics.time.TemporalPoint
import dev.dres.data.model.competition.options.SimpleOption
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.aspects.TemporalSubmissionAspect
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunManager
import dev.dres.run.audit.AuditLogger
import dev.dres.run.audit.LogEventSource
import dev.dres.run.exceptions.IllegalRunStateException
import dev.dres.run.exceptions.IllegalTeamIdException
import dev.dres.run.filter.SubmissionRejectedException
import dev.dres.utilities.FFmpegUtil
import dev.dres.utilities.TimeUtil
import dev.dres.utilities.extensions.sessionId
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SubmissionHandler (val collections: DAO<MediaCollection>, private val itemIndex: DaoIndexer<MediaItem, Pair<UID, String>>, private val segmentIndex: DaoIndexer<MediaItemSegmentList, UID>, private val config: Config): GetRestHandler<SuccessfulSubmissionsStatus>, AccessManagedRestHandler {
    override val permittedRoles = setOf(RestApiRole.PARTICIPANT)
    override val route = "submit"
    override val apiVersion = "v1"

    private val logger = LoggerFactory.getLogger(this.javaClass)

    companion object {
        const val PARAMETER_NAME_COLLECTION = "collection"
        const val PARAMETER_NAME_ITEM = "item"
        const val PARAMETER_NAME_SHOT = "shot"
        const val PARAMETER_NAME_FRAME = "frame"
        const val PARAMETER_NAME_TIMECODE = "timecode"
        const val PARAMETER_NAME_TEXT = "text"
    }


    private fun getRelevantManagers(userId: UID): Set<RunManager> = AccessManager.getRunManagerForUser(userId)

    private fun getActiveRun(userId: UID, ctx: Context): InteractiveRunManager {
        val managers = getRelevantManagers(userId).filterIsInstance(InteractiveRunManager::class.java).filter {
            val rac = RunActionContext.runActionContext(ctx, it)
            it.currentTask(rac)?.isRunning == true
        }
        if (managers.isEmpty()) {
            throw ErrorStatusException(404, "There is currently no eligible competition with an active task.", ctx)
        }

        if (managers.size > 1) {
            throw ErrorStatusException(409, "More than one possible competition found: ${managers.joinToString { it.description.name }}", ctx)
        }

        return managers.first()
    }

    private fun toSubmission(ctx: Context, userId: UID, runManager: InteractiveRunManager, submissionTime: Long): Submission {
        val map = ctx.queryParamMap()

        /* Find team that the user belongs to. */
        val team = runManager.description.teams.find {
            it.users.contains(userId)
        }?.uid ?: throw ErrorStatusException(404, "No team for user '$userId' could not be found.", ctx)

        val rac = RunActionContext.runActionContext(ctx, runManager)

        /* If text is supplied, it supersedes other parameters */
        val text = map[PARAMETER_NAME_TEXT]?.first()
        if (text != null) {
            return Submission.Text(
                team, userId, submissionTime, text
            ).also {
                it.task = runManager.currentTask(rac)
            }
        }

        /* Find collectionId the submission belongs to. */
        val collectionParam = map[PARAMETER_NAME_COLLECTION]?.first()
        val collectionId: UID = when {
            collectionParam != null -> this.collections.find { it.name == collectionParam }?.id
            else -> runManager.currentTaskDescription(rac).mediaCollectionId
        } ?: throw ErrorStatusException(404, "Media collection '$collectionParam' could not be found.", ctx)

        /* Find media item. */
        val itemParam = map[PARAMETER_NAME_ITEM]?.first() ?: throw ErrorStatusException(404, "Parameter '$PARAMETER_NAME_ITEM' is missing but required!'", ctx)
        val item = this.itemIndex[collectionId to itemParam].firstOrNull() ?:
            throw ErrorStatusException(404, "Media item '$itemParam (collection = $collectionId)' could not be found.", ctx)

        val mapToSegment = runManager.currentTaskDescription(rac).taskType.options.any { it.option == SimpleOption.MAP_TO_SEGMENT }
        return when {
            map.containsKey(PARAMETER_NAME_SHOT) && item is MediaItem.VideoItem -> {
                val segmentList = segmentIndex[item.id].firstOrNull() ?: throw ErrorStatusException(400, "Item '${item.name}' not found.", ctx)
                val time = TimeUtil.shotToTime(map[PARAMETER_NAME_SHOT]?.first()!!, segmentList) ?: throw ErrorStatusException(400, "Shot '${item.name}.${map[PARAMETER_NAME_SHOT]?.first()!!}' not found.", ctx)
                Submission.Temporal(team, userId, submissionTime, item, time.first, time.second)
            }
            map.containsKey(PARAMETER_NAME_FRAME) && (item is PlayableMediaItem) -> {
                val time = TemporalPoint.Frame.toMilliseconds(
                    map[PARAMETER_NAME_FRAME]?.first()?.toIntOrNull() ?: throw ErrorStatusException(400, "Parameter '$PARAMETER_NAME_FRAME' must be a number.", ctx),
                    item.fps
                )
                val range = if(mapToSegment && item is MediaItem.VideoItem) {
                    (TimeUtil.timeToSegment(
                        time,
                        segmentIndex[item.id].firstOrNull() ?: throw ErrorStatusException(
                            400,
                            "Item '${item.name}' not found.",
                            ctx
                        )
                    ) ?: throw ErrorStatusException(400, "No segments found for item '${item.name}'.", ctx))
                } else {
                    time to time
                }
                Submission.Temporal(team, userId, submissionTime, item, range.first, range.second)
            }
            map.containsKey(PARAMETER_NAME_TIMECODE) && (item is PlayableMediaItem) -> {
                val time = TemporalPoint.Timecode.timeCodeToMilliseconds(map[PARAMETER_NAME_TIMECODE]?.first()!!, item) ?: throw ErrorStatusException(400, "'${map[PARAMETER_NAME_TIMECODE]?.first()!!}' is not a valid time code", ctx)
                val range = if(mapToSegment && item is MediaItem.VideoItem) {
                    (TimeUtil.timeToSegment(
                        time,
                        segmentIndex[item.id].firstOrNull() ?: throw ErrorStatusException(
                            400,
                            "Item '${item.name}' not found.",
                            ctx
                        )
                    ) ?: throw ErrorStatusException(400, "No segments found for item '${item.name}'.", ctx))
                } else {
                    time to time
                }
                Submission.Temporal(team, userId, submissionTime, item, range.first, range.second)
            }
            else -> Submission.Item(team, userId, submissionTime, item)
        }.also {
            it.task = runManager.currentTask(rac)
        }
    }

    @OpenApi(summary = "Endpoint to accept submissions",
            path = "/api/v1/submit",
            queryParams = [
                OpenApiParam(PARAMETER_NAME_COLLECTION, String::class, "Collection identifier. Optional, in which case the default collection for the run will be considered.", allowEmptyValue = true),
                OpenApiParam(PARAMETER_NAME_ITEM, String::class, "Identifier for the actual media object or media file."),
                OpenApiParam(PARAMETER_NAME_TEXT, String::class, "Text to be submitted. ONLY for tasks with target type TEXT. If this parameter is provided, it superseeds all athers.", allowEmptyValue = true, required = false),
                OpenApiParam(PARAMETER_NAME_FRAME, Int::class, "Frame number for media with temporal progression (e.g. video).", allowEmptyValue = true, required = false),
                OpenApiParam(PARAMETER_NAME_SHOT, Int::class, "Shot number for media with temporal progression (e.g. video).", allowEmptyValue = true, required = false),
                OpenApiParam(PARAMETER_NAME_TIMECODE, String::class, "Timecode for media with temporal progression (e.g. video).", allowEmptyValue = true, required = false),
                OpenApiParam("session", String::class, "Session Token")
            ],
            tags = ["Submission"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessfulSubmissionsStatus::class)]),
                OpenApiResponse("202", [OpenApiContent(SuccessfulSubmissionsStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("412", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): SuccessfulSubmissionsStatus {
        val userId = AccessManager.getUserIdForSession(ctx.sessionId()) ?: throw ErrorStatusException(401, "Authorization required.", ctx)
        val run = getActiveRun(userId, ctx)
        val time = System.currentTimeMillis()
        val submission = toSubmission(ctx, userId, run, time)
        val rac = RunActionContext.runActionContext(ctx, run)

        val result = try {
            run.postSubmission(rac, submission)
        } catch (e: SubmissionRejectedException) {
            throw ErrorStatusException(412, e.message ?: "Submission rejected by submission filter.", ctx)
        } catch (e: IllegalRunStateException) {
            logger.info("Submission was received while Run manager not accepting submissions")
            throw ErrorStatusException(400, "Run manager is in wrong state and cannot accept any more submission.", ctx)
        } catch (e: IllegalTeamIdException) {
            logger.info("Submission with unkown team id '${rac.teamId}' was received")
            throw ErrorStatusException(400, "Run manager does not know the given teamId ${rac.teamId}.", ctx)
        }

        AuditLogger.submission(run.id, run.currentTaskDescription(rac).name, run.currentTask(rac)?.uid, submission, LogEventSource.REST, ctx.sessionId(), ctx.req.remoteAddr)

        if (run.currentTaskDescription(rac).taskType.options.any { it.option == SimpleOption.HIDDEN_RESULTS }) { //pre-generate preview
            generatePreview(submission)
        }

        logger.info("submission ${submission.uid} received status $result")

        return when (result) {
            SubmissionStatus.CORRECT -> SuccessfulSubmissionsStatus(SubmissionStatus.CORRECT, "Submission correct!")
            SubmissionStatus.WRONG -> SuccessfulSubmissionsStatus(SubmissionStatus.WRONG, "Submission incorrect! Try again")
            SubmissionStatus.INDETERMINATE -> {
                ctx.status(202) /* HTTP Accepted. */
                SuccessfulSubmissionsStatus(SubmissionStatus.INDETERMINATE, "Submission received. Waiting for verdict!")
            }
            SubmissionStatus.UNDECIDABLE -> SuccessfulSubmissionsStatus(SubmissionStatus.UNDECIDABLE,"Submission undecidable. Try again!")
        }
    }

    private fun generatePreview(submission: Submission) {
        if (submission !is TemporalSubmissionAspect) {
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

package dev.dres.api.rest.handler.submission

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessfulSubmissionsStatus
import dev.dres.data.model.Config
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.audit.DbAuditLogSource
import dev.dres.data.model.template.task.options.DbTaskOption
import dev.dres.data.model.media.*
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.submissions.DbAnswerType
import dev.dres.run.InteractiveRunManager
import dev.dres.run.audit.AuditLogger
import dev.dres.run.exceptions.IllegalRunStateException
import dev.dres.run.exceptions.IllegalTeamIdException
import dev.dres.run.filter.SubmissionRejectedException
import dev.dres.utilities.FFmpegUtil
import dev.dres.utilities.TimeUtil
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths

/**
 * An [GetRestHandler] used to process [DbSubmission]s.
 *
 * This endpoint strictly considers [DbSubmission]s to contain single [DbAnswerSet]s.
 *
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class SubmissionHandler(private val store: TransientEntityStore, private val config: Config): GetRestHandler<SuccessfulSubmissionsStatus>, AccessManagedRestHandler {

    /** [SubmissionHandler] requires [ApiRole.PARTICIPANT]. */
    override val permittedRoles = setOf(ApiRole.PARTICIPANT)

    /** All [SubmissionHandler]s are part of the v1 API. */
    override val apiVersion = "v2"

    override val route = "submit"

    private val logger = LoggerFactory.getLogger(this.javaClass)

    companion object {
        const val PARAMETER_NAME_COLLECTION = "collection"
        const val PARAMETER_NAME_ITEM = "item"
        const val PARAMETER_NAME_SHOT = "shot"
        const val PARAMETER_NAME_FRAME = "frame"
        const val PARAMETER_NAME_TIMECODE = "timecode"
        const val PARAMETER_NAME_TEXT = "text"
    }

    @OpenApi(summary = "Endpoint to accept submissions",
            path = "/api/v2/submit",
            queryParams = [
                OpenApiParam(PARAMETER_NAME_COLLECTION, String::class, "Collection identifier. Optional, in which case the default collection for the run will be considered.", allowEmptyValue = true),
                OpenApiParam(PARAMETER_NAME_ITEM, String::class, "Identifier for the actual media object or media file."),
                OpenApiParam(PARAMETER_NAME_TEXT, String::class, "Text to be submitted. ONLY for tasks with target type TEXT. If this parameter is provided, it superseeds all athers.", allowEmptyValue = true, required = false),
                OpenApiParam(PARAMETER_NAME_FRAME, Int::class, "Frame number for media with temporal progression (e.g., video).", allowEmptyValue = true, required = false),
                OpenApiParam(PARAMETER_NAME_SHOT, Int::class, "Shot number for media with temporal progression (e.g., video).", allowEmptyValue = true, required = false),
                OpenApiParam(PARAMETER_NAME_TIMECODE, String::class, "Timecode for media with temporal progression (e.g,. video).", allowEmptyValue = true, required = false),
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
            ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): SuccessfulSubmissionsStatus {
        val (s,r) = this.store.transactional {
            val userId = AccessManager.userIdForSession(ctx.sessionToken()) ?: throw ErrorStatusException(401, "Authorization required.", ctx)
            val run = getEligibleRunManager(userId, ctx)
            val time = System.currentTimeMillis()
            val submission = toSubmission(userId, run, time, ctx)
            val rac = RunActionContext.runActionContext(ctx, run)

            val result = try {
                run.postSubmission(rac, submission)
            } catch (e: SubmissionRejectedException) {
                throw ErrorStatusException(412, e.message ?: "Submission rejected by submission filter.", ctx)
            } catch (e: IllegalRunStateException) {
                logger.info("Submission was received while run manager was not accepting submissions.")
                throw ErrorStatusException(400, "Run manager is in wrong state and cannot accept any more submission.", ctx)
            } catch (e: IllegalTeamIdException) {
                logger.info("Submission with unknown team id '${rac.teamId}' was received.")
                throw ErrorStatusException(400, "Run manager does not know the given teamId ${rac.teamId}.", ctx)
            }

            AuditLogger.submission(submission, DbAuditLogSource.REST, ctx.sessionToken(), ctx.req().remoteAddr)
            if (run.currentTaskTemplate(rac).taskGroup.type.options.contains(DbTaskOption.HIDDEN_RESULTS)) { //pre-generate preview
                generatePreview(submission.verdicts.first())
            }
            submission to result
        }

        logger.info("Submission ${s.id} received status $r.")

        return when (r) {
            DbVerdictStatus.CORRECT -> SuccessfulSubmissionsStatus(DbVerdictStatus.CORRECT, "Submission correct!")
            DbVerdictStatus.WRONG -> SuccessfulSubmissionsStatus(DbVerdictStatus.WRONG, "Submission incorrect! Try again")
            DbVerdictStatus.INDETERMINATE -> {
                ctx.status(202) /* HTTP Accepted. */
                SuccessfulSubmissionsStatus(DbVerdictStatus.INDETERMINATE, "Submission received. Waiting for verdict!")
            }
            DbVerdictStatus.UNDECIDABLE -> SuccessfulSubmissionsStatus(DbVerdictStatus.UNDECIDABLE,"Submission undecidable. Try again!")
            else -> throw ErrorStatusException(500, "Unsupported submission status. This is very unusual!", ctx)
        }
    }

    /**
     * Returns the [InteractiveRunManager] that is eligible for the given [UserId] and [Context]
     */
    private fun getEligibleRunManager(userId: UserId, ctx: Context): InteractiveRunManager {
        val managers = AccessManager.getRunManagerForUser(userId).filterIsInstance(InteractiveRunManager::class.java).filter {
            val rac = RunActionContext.runActionContext(ctx, it)
            it.currentTask(rac)?.isRunning == true
        }
        if (managers.isEmpty()) throw ErrorStatusException(404, "There is currently no eligible competition with an active task.", ctx)
        if (managers.size > 1)  throw ErrorStatusException(409, "More than one possible competition found: ${managers.joinToString { it.template.name }}", ctx)
        return managers.first()
    }

    /**
     * Converts the user request tu a [DbSubmission].
     *
     * Creates the associated database entry. Requires an ongoing transaction.
     *
     * @param userId The [UserId] of the user who triggered the [DbSubmission].
     * @param runManager The [InteractiveRunManager]
     * @param submissionTime Time of the submission.
     * @param ctx The HTTP [Context]
     */
    private fun toSubmission(userId: UserId, runManager: InteractiveRunManager, submissionTime: Long, ctx: Context): DbSubmission {
        val map = ctx.queryParamMap()

        /* Find team that the user belongs to. */
        val user = DbUser.query(DbUser::id eq userId).firstOrNull()
            ?: throw ErrorStatusException(404, "No user with ID '$userId' could be found.", ctx)
        val team = runManager.template.teams.filter { it.users.contains(user) }.firstOrNull()
            ?: throw ErrorStatusException(404, "No team for user '$userId' could be found.", ctx)
        val rac = RunActionContext.runActionContext(ctx, runManager)

        /* Create new submission. */
        val submission = DbSubmission.new {
            this.user = user
            this.team = team
            this.timestamp = submissionTime
        }

        /* If text is supplied, it supersedes other parameters */
        val textParam = map[PARAMETER_NAME_TEXT]?.first()
        val itemParam = map[PARAMETER_NAME_ITEM]?.first()
        val currentTaskId = runManager.currentTask(rac)?.id
        val task = DbTask.query(DbTask::id eq currentTaskId).firstOrNull() ?: throw ErrorStatusException(404, "No active task for ID '$currentTaskId' could be found.", ctx)

        /* Create Verdict. */
        val answerSet = DbAnswerSet.new {
            this.status = DbVerdictStatus.INDETERMINATE
            this.task = task
        }
        submission.verdicts.add(answerSet)

        if (textParam != null) {
            answerSet.type = DbAnswerType.TEXT
            answerSet.text = textParam
            return submission
        } else if (itemParam != null) {
            val collection = runManager.currentTaskTemplate(rac).collection /* TODO: Do we need the option to explicitly set the collection name? */
            val mapToSegment = runManager.currentTaskTemplate(rac).taskGroup.type.options.contains(DbTaskOption.MAP_TO_SEGMENT)
            val item = DbMediaItem.query((DbMediaItem::name eq itemParam) and (DbMediaItem::collection eq collection)).firstOrNull()
                ?: throw ErrorStatusException(404, "Parameter '$PARAMETER_NAME_ITEM' is missing but required!'", ctx)
            val range: Pair<Long,Long>? = when {
                map.containsKey(PARAMETER_NAME_SHOT) && item.type == DbMediaType.VIDEO -> {
                    val time = TimeUtil.shotToTime(map[PARAMETER_NAME_SHOT]?.first()!!, item.segments.toList())
                        ?: throw ErrorStatusException(400, "Shot '${item.name}.${map[PARAMETER_NAME_SHOT]?.first()!!}' not found.", ctx)
                    time.first to time.second
                }
                map.containsKey(PARAMETER_NAME_FRAME) && item.type == DbMediaType.VIDEO -> {
                    val fps = item.fps
                        ?: throw IllegalStateException("Missing media item fps information prevented mapping from frame number to milliseconds.")
                    val time = TemporalPoint.Frame.toMilliseconds(
                        map[PARAMETER_NAME_FRAME]?.first()?.toIntOrNull()
                            ?: throw ErrorStatusException(400, "Parameter '$PARAMETER_NAME_FRAME' must be a number.", ctx),
                        fps
                    )
                    if (mapToSegment) {
                        TimeUtil.timeToSegment(time, item.segments.toList())
                            ?: throw ErrorStatusException(400, "No matching segments found for item '${item.name}'.", ctx)
                    } else {
                        time to time
                    }
                }
                map.containsKey(PARAMETER_NAME_TIMECODE) -> {
                    val fps = item.fps
                        ?: throw IllegalStateException("Missing media item fps information prevented mapping from frame number to milliseconds.")
                    val time =
                        TemporalPoint.Timecode.timeCodeToMilliseconds(map[PARAMETER_NAME_TIMECODE]?.first()!!, fps)
                            ?: throw ErrorStatusException(400, "'${map[PARAMETER_NAME_TIMECODE]?.first()!!}' is not a valid time code", ctx)
                   if (mapToSegment) {
                        TimeUtil.timeToSegment(time, item.segments.toList())
                            ?: throw ErrorStatusException(400, "No matching segments found for item '${item.name}'.", ctx)
                    } else {
                        time to time
                    }
                }
                else -> null
            }

            /* Assign information to submission. */
            if (range != null) {
                answerSet.item = item
                answerSet.type = DbAnswerType.TEMPORAL
                answerSet.start = range.first
                answerSet.end = range.second
            } else {
                answerSet.item = item
                answerSet.type = DbAnswerType.ITEM
            }
        } else {
            throw ErrorStatusException(404, "Required submission parameters are missing (content not set)!", ctx)
        }

        return submission
    }

    /**
     * Triggers generation of a preview image for the provided [DbSubmission].
     *
     * @param answerSet The [DbAnswerSet] to generate preview for.
     */
    private fun generatePreview(answerSet: DbAnswerSet) {
        if (answerSet.type != DbAnswerType.TEMPORAL) return
        if (answerSet.item == null) return
        val destinationPath = Paths.get(this.config.cachePath, "previews", answerSet.item!!.collection.name, answerSet.item!!.name, "${answerSet.start}.jpg")
        if (Files.exists(destinationPath)){
            return
        }
        FFmpegUtil.extractFrame(answerSet.item!!.pathToOriginal(), answerSet.start!!, destinationPath)
    }
}
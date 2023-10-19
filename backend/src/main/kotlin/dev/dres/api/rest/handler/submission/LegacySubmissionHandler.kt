package dev.dres.api.rest.handler.submission

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.evaluation.submission.*
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessfulSubmissionsStatus
import dev.dres.api.rest.types.template.tasks.options.ApiSubmissionOption
import dev.dres.api.rest.types.template.tasks.options.ApiTaskOption
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.template.task.options.DbTaskOption
import dev.dres.data.model.media.*
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.data.model.submissions.*
import dev.dres.mgmt.cache.CacheManager
import dev.dres.run.InteractiveRunManager
import dev.dres.run.exceptions.IllegalRunStateException
import dev.dres.run.exceptions.IllegalTeamIdException
import dev.dres.run.filter.SubmissionRejectedException
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import org.slf4j.LoggerFactory

/**
 * An [GetRestHandler] used to process [DbSubmission]s.
 *
 * This endpoint strictly considers [DbSubmission]s to contain single [DbAnswerSet]s.
 *
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class LegacySubmissionHandler(private val store: TransientEntityStore, private val cache: CacheManager) :
    GetRestHandler<SuccessfulSubmissionsStatus>, AccessManagedRestHandler {

    /** [LegacySubmissionHandler] requires [ApiRole.PARTICIPANT]. */
    override val permittedRoles = setOf(ApiRole.PARTICIPANT)

    /** All [LegacySubmissionHandler]s are part of the v1 API. */
    override val apiVersion = "v1"

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

    @OpenApi(
        summary = "Endpoint to accept submissions",
        path = "/api/v1/submit",
        operationId = OpenApiOperation.AUTO_GENERATE,
        queryParams = [
            OpenApiParam(
                PARAMETER_NAME_COLLECTION,
                String::class,
                "Collection identifier. Optional, in which case the default collection for the run will be considered.",
                allowEmptyValue = true
            ),
            OpenApiParam(PARAMETER_NAME_ITEM, String::class, "Identifier for the actual media object or media file."),
            OpenApiParam(
                PARAMETER_NAME_TEXT,
                String::class,
                "Text to be submitted. ONLY for tasks with target type TEXT. If this parameter is provided, it superseeds all athers.",
                allowEmptyValue = true,
                required = false
            ),
            OpenApiParam(
                PARAMETER_NAME_FRAME,
                Int::class,
                "Frame number for media with temporal progression (e.g., video).",
                allowEmptyValue = true,
                required = false
            ),
            OpenApiParam(
                PARAMETER_NAME_SHOT,
                Int::class,
                "Shot number for media with temporal progression (e.g., video).",
                allowEmptyValue = true,
                required = false
            ),
            OpenApiParam(
                PARAMETER_NAME_TIMECODE,
                String::class,
                "Timecode for media with temporal progression (e.g,. video).",
                allowEmptyValue = true,
                required = false
            ),
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
        methods = [HttpMethod.GET],
        deprecated = true,
        description = "This has been the submission endpoint for version 1. Please refrain from using it and migrate to the v2 endpoint."
    )
    override fun doGet(ctx: Context): SuccessfulSubmissionsStatus {
        /* Obtain run action context and parse submission. */
        val rac = ctx.runActionContext()
        val (run, submission) = this.store.transactional(true) {
            val run = getEligibleRunManager(rac, ctx)
            run to toSubmission(rac, run, ctx)
        }

        /* Post submission. */
        val apiSubmission = try {
            run.postSubmission(rac, submission)
        } catch (e: SubmissionRejectedException) {
            throw ErrorStatusException(412, e.message ?: "Submission rejected by submission filter.", ctx)
        } catch (e: IllegalRunStateException) {
            logger.info("Submission was received while run manager was not accepting submissions.")
            throw ErrorStatusException(400, "Run manager is in wrong state and cannot accept any more submission.", ctx)
        } catch (e: IllegalTeamIdException) {
            logger.info("Submission with unknown team id '${submission.teamId}' was received.")
            throw ErrorStatusException(400, "Run manager does not know the given teamId ${submission.teamId}.", ctx)
        }

        /* Lookup verdict for submission and return it. */
        return when (apiSubmission.answers.first().status) {
            ApiVerdictStatus.CORRECT -> SuccessfulSubmissionsStatus(ApiVerdictStatus.CORRECT, "Submission correct!")
            ApiVerdictStatus.WRONG -> SuccessfulSubmissionsStatus(
                ApiVerdictStatus.WRONG,
                "Submission incorrect! Try again"
            )

            ApiVerdictStatus.INDETERMINATE -> {
                ctx.status(202) /* HTTP Accepted. */
                SuccessfulSubmissionsStatus(ApiVerdictStatus.INDETERMINATE, "Submission received. Waiting for verdict!")
            }

            ApiVerdictStatus.UNDECIDABLE -> SuccessfulSubmissionsStatus(
                ApiVerdictStatus.UNDECIDABLE,
                "Submission undecidable. Try again!"
            )

            else -> throw ErrorStatusException(500, "Unsupported submission status. This is very unusual!", ctx)
        }

    }

    /**
     * Returns the [InteractiveRunManager] that is eligible for the given [UserId] and [Context]
     *
     * @param rac The [RunActionContext] used for the lookup.
     * @param ctx The current [Context].
     */
    private fun getEligibleRunManager(rac: RunActionContext, ctx: Context): InteractiveRunManager {
        val managers =
            AccessManager.getRunManagerForUser(rac.userId).filterIsInstance(InteractiveRunManager::class.java).filter {
                it.currentTask(rac)?.isRunning == true
            }
        if (managers.isEmpty()) throw ErrorStatusException(
            404,
            "There is currently no eligible evaluation with an active task.",
            ctx
        )
        if (managers.size > 1) throw ErrorStatusException(
            409,
            "More than one possible evaluation found: ${managers.joinToString { it.template.name }}",
            ctx
        )
        return managers.first()
    }

    /**
     * Converts the user request tu a [ApiClientSubmission].
     *
     * @param rac The [RunActionContext] used for the conversion.
     * @param runManager The [InteractiveRunManager]
     * @param ctx The HTTP [Context]
     */
    private fun toSubmission(
        rac: RunActionContext,
        runManager: InteractiveRunManager,
        ctx: Context
    ): ApiClientSubmission {
        val map = ctx.queryParamMap()

        /* If text is supplied, it supersedes other parameters */
        val textParam = map[PARAMETER_NAME_TEXT]?.first()
        val itemParam = map[PARAMETER_NAME_ITEM]?.first()

        val answer = if (textParam != null) {
            ApiClientAnswer(text = textParam)
        } else if (itemParam != null) {
            val collection =
                runManager.currentTaskTemplate(rac).collectionId /* TODO: Do we need the option to explicitly set the collection name? */
            val item = DbMediaCollection.query(DbMediaCollection::id eq collection)
                .firstOrNull()?.items?.filter { it.name eq itemParam }?.firstOrNull()
                ?: throw ErrorStatusException(404, "Item '$PARAMETER_NAME_ITEM' not found'", ctx)
            val range: Pair<Long, Long>? = when {
                map.containsKey(PARAMETER_NAME_SHOT) && item.type == DbMediaType.VIDEO -> {
                    val shot = map[PARAMETER_NAME_SHOT]?.first()!!
                    val time = item.segments.filter { it.name eq shot }
                        .firstOrNull()?.range?.toMilliseconds()//TimeUtil.shotToTime(map[PARAMETER_NAME_SHOT]?.first()!!, item.segments.toList())
                        ?: throw ErrorStatusException(
                            400,
                            "Shot '${item.name}.${map[PARAMETER_NAME_SHOT]?.first()!!}' not found.",
                            ctx
                        )
                    time.first to time.second
                }

                map.containsKey(PARAMETER_NAME_FRAME) && item.type == DbMediaType.VIDEO -> {
                    val fps = item.fps
                        ?: throw IllegalStateException("Missing media item fps information prevented mapping from frame number to milliseconds.")
                    val time = TemporalPoint.Frame(
                        map[PARAMETER_NAME_FRAME]?.first()?.toIntOrNull()
                            ?: throw ErrorStatusException(
                                400,
                                "Parameter '$PARAMETER_NAME_FRAME' must be a number.",
                                ctx
                            ),
                        fps
                    )
                    val ms = time.toMilliseconds()
                    ms to ms

                }

                map.containsKey(PARAMETER_NAME_TIMECODE) -> {
                    val fps = item.fps
                        ?: throw IllegalStateException("Missing media item fps information prevented mapping from frame number to milliseconds.")
                    val time =
                        TemporalPoint.Millisecond(
                            TemporalPoint.Timecode.timeCodeToMilliseconds(map[PARAMETER_NAME_TIMECODE]?.first()!!, fps)
                                ?: throw ErrorStatusException(
                                    400,
                                    "'${map[PARAMETER_NAME_TIMECODE]?.first()!!}' is not a valid time code",
                                    ctx
                                )
                        )
                    val ms = time.toMilliseconds()
                    ms to ms

                }

                else -> null
            }

            /* Assign information to submission. */
            if (range != null) {
                ApiClientAnswer(mediaItemName = itemParam, start = range.first, end = range.second)
            } else {
                ApiClientAnswer(mediaItemName = itemParam)
            }
        } else {
            throw ErrorStatusException(404, "Required submission parameters are missing (content not set)!", ctx)
        }

        /* Generate and return ApiClientSubmission. */
        return ApiClientSubmission(listOf(ApiClientAnswerSet(answers = listOf(answer))))
    }

    /**
     * Triggers generation of a preview image for the provided [DbSubmission].
     *
     * @param answerSet The [DbAnswerSet] to generate preview for.
     */
    private fun generatePreview(answerSet: AnswerSet) {
        if (answerSet.answers().firstOrNull()?.type() != AnswerType.TEMPORAL) return
        if (answerSet.answers().firstOrNull()?.item == null) return
        val item =
            DbMediaItem.query((DbMediaItem::id eq answerSet.answers().firstOrNull()?.item!!.mediaItemId)).firstOrNull()
                ?: return
        this.cache.asyncPreviewImage(item, answerSet.answers().firstOrNull()?.start ?: 0)
    }
}

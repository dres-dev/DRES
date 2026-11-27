package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.DRES
import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.preview.GetMediaGrantHandler
import dev.dres.api.rest.handler.preview.MediaGrantPayload
import dev.dres.api.rest.handler.preview.MediaGrantRegistry
import dev.dres.api.rest.handler.preview.MediaRequestGuard
import dev.dres.api.rest.types.ViewerInfo
import dev.dres.api.rest.types.template.tasks.ApiHintContent
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.task.ApiContentElement
import dev.dres.api.rest.types.task.ApiContentType
import dev.dres.api.rest.types.template.tasks.ApiHint
import dev.dres.api.rest.types.template.tasks.ApiHintType
import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.mgmt.cache.CacheManager
import dev.dres.run.*
import dev.dres.utilities.concurrent.RenderAwait
import dev.dres.utilities.extensions.*
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.util.*

/**
 * A [AbstractEvaluationViewerHandler] that returns the currently active [DbTaskTemplate].
 *
 * If a [DbTask] is being executed, the method returns the [DbTaskTemplate] for that [DbTask].
 * Otherwise, the selected [DbTaskTemplate] is returned (active task vs. task template navigation).
 *
 * Only eligible for [InteractiveRunManager]s.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class GetTaskHintHandler(private val store: TransientEntityStore, private val cache: CacheManager) :
    AbstractEvaluationViewerHandler(), GetRestHandler<ApiHintContent> {

    override val route = "evaluation/{evaluationId}/template/task/{taskTemplateId}/hint"

    @OpenApi(
        summary = "Returns the task hint for the specified task template in the context of the provided evaluation.",
        path = "/api/v2/evaluation/{evaluationId}/template/task/{taskTemplateId}/hint",
        tags = ["Evaluation"],
        operationId = "getHintForTaskTemplateId",
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The ID of the evaluation.", required = true),
            OpenApiParam("taskTemplateId", String::class, "The ID of the task template.", required = true)
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiHintContent::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiHintContent {
        val taskTemplateId = ctx.pathParamMap()["taskTemplateId"] ?: throw ErrorStatusException(400, "Parameter 'taskTemplateId' not specified.", ctx)
        val rac = ctx.runActionContext()

        return this.store.transactional(true) {
            val manager = ctx.eligibleManagerForId<RunManager>()

            /* Performs (run manager specific) gate-keeping. */
            manager.executeGatekeeper(ctx, taskTemplateId, rac)

            /* Hints embed media payloads, so the same budget as for direct media retrieval applies. */
            MediaRequestGuard.enforce(ctx, "task-hint", taskTemplateId)

            /* Find template and load hint. */
            val template = manager.evaluation.template.tasks.find { it.id == taskTemplateId } ?: throw ErrorStatusException(404, "Task template not found.", ctx)
            try {
                /* Hints are participant-specific payloads; a shared cache must never retain them. */
                ctx.header("Cache-Control", "private, no-store")
                template.toTaskHint(ctx, ctx.sessionToken(), manager.id, taskTemplateId)
            } catch (e: FileNotFoundException) {
                throw ErrorStatusException(404, "Query object cache file not found!", ctx)
            } catch (ioe: IOException) {
                throw ErrorStatusException(500, "Exception when reading query object cache file.", ctx)
            }
        }
    }

    /**
     * This function executes [RunManager] implementation specific checks and logic that is necessary in the context of this API call.
     *
     * @param ctx [Context]
     * @param taskTemplateId The ID of the task template that is accessed.
     * @param rac [RunActionContext]
     */
    private fun RunManager.executeGatekeeper(ctx: Context, taskTemplateId: String, rac: RunActionContext) {
        when(this) {
            is InteractiveRunManager -> {
                if (!AccessManager.canObserveRunState(ctx, this.runProperties)) {
                    throw ErrorStatusException(403, "Access denied.", ctx)
                }

                if (ctx.isParticipant() || ctx.isAdmin()) {
                    this.viewerPreparing(taskTemplateId, rac, ViewerInfo(ctx))
                }

                /* Participants and judges only receive hints once a task run exists. */
                val taskRunExists = this.tasks(rac).any { it.taskTemplateId == taskTemplateId }
                if (!isTaskHintAccessAllowed(ctx.isParticipant(), ctx.isJudge(), taskRunExists)) {
                    throw ErrorStatusException(
                        403,
                        "Access denied; task hint cannot be displayed before the task run exists.",
                        ctx
                    )
                }
            }
            else -> { /* No op */ }
        }
    }

    /**
     * Generates and returns a [ApiHintContent] object to be used by thi RESTful interface. Requires a valid transaction.
     *
     * @return [ApiHintContent]
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    private fun ApiTaskTemplate.toTaskHint(ctx: Context, session: String?, evaluationId: String, taskTemplateId: String): ApiHintContent = store.transactional(true){
        val sequence = this.hints.groupBy { it.type }.flatMap { (_, hints) ->
            var index = 0
            hints.sortedBy { it.start ?: 0 }.flatMap {
                val ret = mutableListOf(it.toContentElement(ctx, session, evaluationId, taskTemplateId))
                if (it.end != null) {
                    if (index == (hints.size - 1)) {
                        ret.add(ApiContentElement(contentType = ret.first().contentType, offset = it.end))
                    } else if ((hints[index + 1].start ?: 0) > it.end) {
                        ret.add(ApiContentElement(contentType = ret.first().contentType, offset = it.end))
                    }
                }
                ++index
                ret
            }
        }
        ApiHintContent(this.id!!, sequence, false)
    }

    private fun ApiHint.toContentElement(ctx: Context, session: String?, evaluationId: String, taskTemplateId: String): ApiContentElement {

        //TODO find a better place for this lookup
        val item = this.item?.let { item ->
            DbMediaItem.filter { it.mediaItemId eq item.mediaItemId }.firstOrNull()
        }
        /*
         * The stored range is already in milliseconds (`TemplateManager` converts on import), so the frame
         * rate is only needed for a source that expresses its bounds in frames. This used to be
         * `toTemporalRange(item.fps!!)`, which threw a NullPointerException for any item without an `fps`
         * value, and which made a hint and a target describing the same segment derive their timestamps by
         * different routes.
         */
        val range = this.range?.toMilliseconds(item?.fps ?: 0.0f)

        val content = when (this.type) {
            ApiHintType.EMPTY -> ""
            ApiHintType.TEXT -> this.description
            ApiHintType.VIDEO -> {
                /*
                 * The template is stored data, not a programmer's invariant: a hint can be imported or
                 * edited without a range. The caller is a viewer asking for a legitimate task, so this is a
                 * server-side data problem (502-adjacent) reported as 500 with an actionable message rather
                 * than an "programmer's error" stack trace.
                 */
                val (start, end) = range
                    ?: throw ErrorStatusException(
                        500,
                        "This task's hint is misconfigured: the video hint has no temporal range. Contact the evaluation's organiser.",
                        ctx
                    )
                val path = if (item != null) {
                    RenderAwait.await(
                        this@GetTaskHintHandler.cache.asyncPreviewVideo(item, start, end),
                        ctx,
                        "task hint video"
                    )
                } else {
                    val source = DRES.EXTERNAL_ROOT.resolve(
                        this.path
                            ?: throw ErrorStatusException(
                                500,
                                "This task's hint is misconfigured: the video hint names neither a media item nor an external file. Contact the evaluation's organiser.",
                                ctx
                            )
                    )
                    RenderAwait.await(
                        this@GetTaskHintHandler.cache.asyncPreviewVideo(source, start, end),
                        ctx,
                        "task hint video"
                    )
                }
                if (Files.exists(path)) {
                    this@GetTaskHintHandler.grantFor(path, session, evaluationId, taskTemplateId)
                } else {
                    null
                }
            }
            ApiHintType.IMAGE -> {
                val path = if (item != null) {
                    RenderAwait.await(
                        this@GetTaskHintHandler.cache.asyncPreviewImage(item),
                        ctx,
                        "task hint image"
                    )
                } else {
                    RenderAwait.await(
                        this@GetTaskHintHandler.cache.asyncPreviewImage(
                            DRES.EXTERNAL_ROOT.resolve(
                                this.path
                                    ?: throw ErrorStatusException(
                                        500,
                                        "This task's hint is misconfigured: the image hint names neither a media item nor an external file. Contact the evaluation's organiser.",
                                        ctx
                                    )
                            )
                        ),
                        ctx,
                        "task hint image"
                    )
                }
                if (Files.exists(path)) {
                    if (path.toString().endsWith(".jpg", ignoreCase = true)) {
                        this@GetTaskHintHandler.grantFor(path, session, evaluationId, taskTemplateId)
                    } else { //should never happen
                        null
                    }
                } else {
                    null
                }
            }
        }

        val contentType = when (this.type) {
            ApiHintType.EMPTY -> ApiContentType.EMPTY
            ApiHintType.TEXT -> ApiContentType.TEXT
            ApiHintType.VIDEO -> ApiContentType.VIDEO
            ApiHintType.IMAGE -> ApiContentType.IMAGE
        }

        /* Media payloads are handed over as a session-bound grant URL rather than inline base64, so
         * the browser can fetch, cache and prefetch them while the payload itself stays scoped and
         * revocable. Text stays inline; media never carries bytes here. */
        val isMedia = contentType == ApiContentType.VIDEO || contentType == ApiContentType.IMAGE
        return ApiContentElement(
            contentType = contentType,
            content = if (isMedia) null else content,
            offset = this.start ?: 0L,
            mediaUrl = if (isMedia) content else null
        )
    }

    /**
     * Mints a [MediaGrant] for an already rendered cache file and returns its URL path.
     *
     * Returns null when the request has no session to bind the grant to; such a caller would not be
     * able to redeem it anyway, and inline base64 would contradict the point of the grant.
     *
     * @param path The prepared cache file.
     * @param session The session that requested the hint.
     * @param evaluationId The evaluation the hint belongs to.
     * @param taskTemplateId The task the hint belongs to.
     * @return The grant URL path, or null.
     */
    private fun grantFor(path: java.nio.file.Path, session: String?, evaluationId: String, taskTemplateId: String): String? {
        if (session == null) {
            return null
        }
        val grant = MediaGrantRegistry.mint(
            session = session,
            evaluationId = evaluationId,
            taskTemplateId = taskTemplateId,
            payload = MediaGrantPayload.Preview(path)
        )
        return GetMediaGrantHandler.grantPath(grant)
    }
}

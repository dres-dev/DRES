package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.preview.GetMediaGrantHandler
import dev.dres.api.rest.handler.preview.MediaGrantPayload
import dev.dres.api.rest.handler.preview.MediaGrantRegistry
import dev.dres.api.rest.handler.preview.MediaRequestGuard
import dev.dres.utilities.extensions.sessionToken
import dev.dres.api.rest.types.evaluation.ApiTaskStatus
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.utilities.extensions.isParticipant
import dev.dres.api.rest.types.template.tasks.ApiTargetContent
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.task.ApiContentElement
import dev.dres.api.rest.types.task.ApiContentType
import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.data.model.media.DbMediaType
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.data.model.template.task.DbHint
import dev.dres.data.model.template.task.DbTargetType
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.template.task.DbTaskTemplateTarget
import dev.dres.mgmt.cache.CacheManager
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunManager
import dev.dres.utilities.concurrent.RenderAwait
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.util.*

/**
 * A [GetRestHandler] to access the target of a particular task.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class GetTaskTargetHandler(private val store: TransientEntityStore, private val cache: CacheManager) : AbstractEvaluationViewerHandler(), GetRestHandler<ApiTargetContent> {

    override val route = "evaluation/{evaluationId}/template/task/{taskTemplateId}/target"

    @OpenApi(
        summary = "Returns the task target for the specified task template in the context of the provided evaluation.",
        path = "/api/v2/evaluation/{evaluationId}/template/task/{taskTemplateId}/target",
        operationId = "getTargetForTaskTemplateId",
        tags = ["Evaluation"],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The ID of the evaluation.", required = true),
            OpenApiParam("taskTemplateId", String::class, "The ID of the task template.", required = true)
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiTargetContent::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiTargetContent {
        val taskTemplateId = ctx.pathParamMap()["taskTemplateId"] ?: throw ErrorStatusException(400, "Parameter 'taskTemplateId' not specified.", ctx)
        val rac = ctx.runActionContext()

        return this.store.transactional (true) {
            val manager = ctx.eligibleManagerForId<RunManager>()

            /* Performs (run manager specific) gate-keeping. */
            manager.executeGatekeeper(ctx, taskTemplateId, rac)

            /* Targets embed media payloads, so the same budget as for direct media retrieval applies. */
            MediaRequestGuard.enforce(ctx, "task-target", taskTemplateId)

            /* Accesses template and converts it to target. */
            val template = manager.evaluation.template.tasks.find { it.id == taskTemplateId }
                ?: throw ErrorStatusException(404, "Task template not found.", ctx)
            try {
                /* Targets contain the ground truth; never let a shared cache retain them. */
                ctx.header("Cache-Control", "private, no-store")
                template.toTaskTarget(ctx, ctx.sessionToken(), manager.id, taskTemplateId)
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
                    throw ErrorStatusException(403, "Access denied; you are not authorised to access the specified task target.", ctx)
                }
                if (!this.tasks(rac).any { it.taskTemplateId == taskTemplateId && it.status == ApiTaskStatus.ENDED }) {
                    throw ErrorStatusException(401, "Access denied; task target cannot be display if task has not been run yet.", ctx)
                }
            }
            else -> { /* No op */ }
        }
    }

    /**
     * Generates and returns a [ApiTargetContent] object to be used by the RESTful interface.
     *
     * Requires a valid transaction.
     *
     * @return [ApiTargetContent]
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    private fun ApiTaskTemplate.toTaskTarget(ctx: Context, session: String?, evaluationId: String, taskTemplateId: String): ApiTargetContent { //TODO there must be a better way to do this
        var cummulativeOffset = 0L
        val sequence = DbTaskTemplate.filter{it.templateId eq this@toTaskTarget.id}.firstOrNull()?.targets?.asSequence()?.flatMap {
            val qce = it.toQueryContentElement(ctx, session, evaluationId, taskTemplateId)
            /* The content */
            val content = ApiContentElement(qce.contentType, qce.content, cummulativeOffset, qce.mediaUrl)
            /* Calculate offset of pause */
            cummulativeOffset += Math.floorDiv(it.range?.durationMs() ?: 10000L, 1000L) + 1L
            /* Pause at the end of the content's length (or 10s for non-temporal content */
            val pause = ApiContentElement(ApiContentType.EMPTY, null, cummulativeOffset)
            /* Expand cummulativeOffset by one second (pause) */
            cummulativeOffset += 1L
            listOf(content, pause)
        }?.toList() ?: emptyList()
        return ApiTargetContent(this.id!!, sequence)
    }

    /**
     * Generates and returns a [ApiContentElement] object of this [DbHint] to be used by the RESTful interface.
     *
     * @return [ApiContentElement]
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    private fun DbTaskTemplateTarget.toQueryContentElement(ctx: Context, session: String?, evaluationId: String, taskTemplateId: String): ApiContentElement {
        var payload: MediaGrantPayload? = null
        val (content, type) = when (this.type) {
            DbTargetType.JUDGEMENT,
            DbTargetType.JUDGEMENT_WITH_VOTE -> null to ApiContentType.EMPTY
            DbTargetType.TEXT -> this.text to ApiContentType.TEXT
            DbTargetType.MEDIA_ITEM -> {
                val type = when (this.item?.type) {
                    DbMediaType.VIDEO -> ApiContentType.VIDEO
                    DbMediaType.IMAGE -> ApiContentType.IMAGE
                    else -> throw IllegalStateException("Invalid target description; type indicates presence of media item but item seems unsupported or unspecified.")
                }
                val item = this.item
                /* The caller is entitled to the ground truth here (the gatekeeper already verified that
                 * the task has ENDED), so the original itself is granted, not a preview. */
                if (item != null && Files.exists(item.pathToOriginal())) {
                    payload = MediaGrantPayload.Item(item.mediaItemId)
                }
                null to type
            }
            DbTargetType.MEDIA_ITEM_TEMPORAL_RANGE -> {
                /* Stored template data can be incomplete; report it as a server-side data problem instead
                 * of an internal assertion, so an operator can act on the message. */
                val item = this.item ?: throw ErrorStatusException(
                    500,
                    "This task's target is misconfigured: the target names no media item. Contact the evaluation's organiser.",
                    ctx
                )
                val start = this.start ?: throw ErrorStatusException(
                    500,
                    "This task's target is misconfigured: the target has no start timestamp. Contact the evaluation's organiser.",
                    ctx
                )
                val end = this.end ?: throw ErrorStatusException(
                    500,
                    "This task's target is misconfigured: the target has no end timestamp. Contact the evaluation's organiser.",
                    ctx
                )
                /* Preparation normally has this cached already, but a bound keeps a slow render from pinning
                 * the worker thread; a 503 with Retry-After is better than an unbounded wait. */
                val path = RenderAwait.await(
                    this@GetTaskTargetHandler.cache.asyncPreviewVideo(item, start, end),
                    ctx,
                    "task target video"
                )
                if (Files.exists(path)) {
                    payload = MediaGrantPayload.Preview(path)
                }
                null to ApiContentType.VIDEO
            }
            else -> throw IllegalStateException("The content type ${this.type.description} is not supported.")
        }

        /* Media is delivered through a session-bound grant URL instead of inline base64; text and
         * judgement targets stay as they are. */
        val mediaUrl = payload?.let { this@GetTaskTargetHandler.grantUrl(it, session, evaluationId, taskTemplateId) }
        return ApiContentElement(contentType = type, content = content, offset = 0L, mediaUrl = mediaUrl)
    }

    /**
     * Mints a [MediaGrant] for [payload] and returns its URL path, or null when the request has no
     * session to bind the grant to.
     *
     * @param payload The payload to grant.
     * @param session The session that requested the target.
     * @param evaluationId The evaluation the target belongs to.
     * @param taskTemplateId The task the target belongs to.
     * @return The grant URL path, or null.
     */
    private fun grantUrl(payload: MediaGrantPayload, session: String?, evaluationId: String, taskTemplateId: String): String? {
        if (session == null) {
            return null
        }
        val grant = MediaGrantRegistry.mint(
            session = session,
            evaluationId = evaluationId,
            taskTemplateId = taskTemplateId,
            payload = payload
        )
        return GetMediaGrantHandler.grantPath(grant)
    }
}

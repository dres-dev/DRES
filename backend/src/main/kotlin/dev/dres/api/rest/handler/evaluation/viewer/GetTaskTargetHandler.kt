package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.GetRestHandler
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

            /* Accesses template and converts it to target. */
            val template = manager.evaluation.template.tasks.find { it.id == taskTemplateId }
                ?: throw ErrorStatusException(404, "Task template not found.", ctx)
            try {
                ctx.header("Cache-Control", "public, max-age=300") //can be cached for 5 minutes
                template.toTaskTarget()
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
                if (!this.runProperties.participantCanView && ctx.isParticipant()) {
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
    private fun ApiTaskTemplate.toTaskTarget(): ApiTargetContent { //TODO there must be a better way to do this
        var cummulativeOffset = 0L
        val sequence = DbTaskTemplate.filter{it.templateId eq this@toTaskTarget.id}.firstOrNull()?.targets?.asSequence()?.flatMap {
            val qce = it.toQueryContentElement()
            /* The content */
            val content = ApiContentElement(qce.contentType, qce.content, cummulativeOffset)
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
    private fun DbTaskTemplateTarget.toQueryContentElement(): ApiContentElement {
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
                val filePath = this.item?.pathToOriginal()
                if (filePath != null && Files.exists(filePath)) {
                    Base64.getEncoder().encodeToString(Files.readAllBytes(filePath))
                } else {
                    null
                } to type
            }
            DbTargetType.MEDIA_ITEM_TEMPORAL_RANGE -> {
                val item = this.item ?: throw IllegalStateException("DbHint of type IMAGE is expected to hold a valid media item but doesn't! This is a programmer's error!")
                val start = this.start ?: throw IllegalStateException("DbHint of type VIDEO is expected to hold a valid start timestamp but doesn't! This is a programmer's error!")
                val end = this.end ?: throw IllegalStateException("DbHint of type VIDEO is expected to hold a valid end timestamp but doesn't!! This is a programmer's error!")
                val path = this@GetTaskTargetHandler.cache.asyncPreviewVideo(item, start, end).get() /* This should return immediately, since the previews have been prepared. */
                if (Files.exists(path)) {
                    Base64.getEncoder().encodeToString(Files.readAllBytes(path))
                } else {
                    null
                } to ApiContentType.VIDEO
            }
            else -> throw IllegalStateException("The content type ${this.type.description} is not supported.")
        }
        return ApiContentElement(contentType = type, content = content, offset = 0L)
    }
}

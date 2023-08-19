import dev.dres.DRES
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.api.rest.handler.evaluation.viewer.AbstractEvaluationViewerHandler
import dev.dres.api.rest.types.ViewerInfo
import dev.dres.utilities.extensions.isParticipant
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
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.data.model.template.task.DbHint
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.mgmt.cache.CacheManager
import dev.dres.run.InteractiveRunManager
import dev.dres.utilities.extensions.isAdmin
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query
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

    override val route = "evaluation/{evaluationId}/hint/{taskId}"

    @OpenApi(
        summary = "Returns the task hint for the specified task.",
        path = "/api/v2/evaluation/{evaluationId}/hint/{taskId}",
        tags = ["Evaluation"],
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true),
            OpenApiParam("taskId", String::class, "The task ID.", required = true)
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
        val taskId = ctx.pathParamMap()["taskId"] ?: throw ErrorStatusException(400, "Parameter 'taskId' not specified.", ctx)
        val rac = ctx.runActionContext()

        return this.store.transactional(true) {
            val manager = ctx.eligibleManagerForId<InteractiveRunManager>()
            if (!manager.runProperties.participantCanView && ctx.isParticipant()) {
                throw ErrorStatusException(403, "Access Denied", ctx)
            }

            val currentTaskDescription = manager.currentTaskTemplate(rac)
            val template = if (currentTaskDescription.id == taskId) {
                currentTaskDescription
            } else {
                manager.taskForId(rac, taskId)?.template ?: throw ErrorStatusException(
                    404,
                    "Task with specified ID $taskId does not exist.",
                    ctx
                )
            }

            if(ctx.isParticipant() || ctx.isAdmin()) {
                manager.viewerPreparing(
                    taskId, rac, ViewerInfo(
                        ctx.sessionToken()!!,
                        ctx.ip()
                    )
                )
            }

            try {
                ctx.header("Cache-Control", "public, max-age=300") //can be cached for 5 minutes
                template.toTaskHint()
            } catch (e: FileNotFoundException) {
                throw ErrorStatusException(404, "Query object cache file not found!", ctx)
            } catch (ioe: IOException) {
                throw ErrorStatusException(500, "Exception when reading query object cache file.", ctx)
            }


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
//    private fun DbTaskTemplate.toTaskHint(): ApiHintContent {
//        val sequence = this.hints.asSequence().groupBy { it.type }.flatMap { group ->
//            var index = 0
//            group.value.sortedBy { it.start ?: 0 }.flatMap {
//                val ret = mutableListOf(it.toContentElement())
//                if (it.end != null) {
//                    if (index == (group.value.size - 1)) {
//                        ret.add(ApiContentElement(contentType = ret.first().contentType, offset = it.end!!))
//                    } else if ((group.value[index + 1].start ?: 0) > it.end!!) {
//                        ret.add(ApiContentElement(contentType = ret.first().contentType, offset = it.end!!))
//                    }
//                }
//                index += 1
//                ret
//            }
//        }
//        return ApiHintContent(this.id, sequence, false)
//    }

    private fun ApiTaskTemplate.toTaskHint(): ApiHintContent = store.transactional(true){
        val sequence = this.hints.groupBy { it.type }.flatMap { (type, hints) ->
            var index = 0
            hints.sortedBy { it.start ?: 0 }.flatMap {
                val ret = mutableListOf(it.toContentElement())
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

    /**
     * Generates and returns a [ApiContentElement] object of this [DbHint] to be used by the RESTful interface.
     *
     * @return [ApiContentElement]
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
//    private fun DbHint.toContentElement(): ApiContentElement {
//        val content = when (this.type) {
//            DbHintType.IMAGE -> {
//                val path = if (this.item != null) {
//                    this@GetTaskHintHandler.cache.asyncPreviewImage(this.item!!)
//                        .get() /* This should return immediately, since the previews have been prepared. */
//                } else {
//                    this@GetTaskHintHandler.cache.asyncPreviewImage(
//                        DRES.EXTERNAL_ROOT.resolve(
//                            this.path
//                                ?: throw IllegalStateException("DbHint of type IMAGE is expected to hold a valid media item or external path but it doesn't! This is a programmer's error!")
//                        )
//                    ).get()
//                }
//                if (Files.exists(path)) {
//                    if (path.toString().endsWith(".jpg", ignoreCase = true)) {
//                        Base64.getEncoder().encodeToString(Files.readAllBytes(path))
//                    } else { //should never happen
//                        null
//                    }
//                } else {
//                    null
//                }
//            }
//
//            DbHintType.VIDEO -> {
//                val start = this.temporalRangeStart
//                    ?: throw IllegalStateException("DbHint of type VIDEO is expected to hold a valid start timestamp but doesn't! This is a programmer's error!")
//                val end = this.temporalRangeEnd
//                    ?: throw IllegalStateException("DbHint of type VIDEO is expected to hold a valid end timestamp but doesn't!! This is a programmer's error!")
//                val path = if (this.item != null) {
//                    this@GetTaskHintHandler.cache.asyncPreviewVideo(this.item!!, start, end)
//                        .get() /* This should return immediately, since the previews have been prepared. */
//                } else {
//                    val source = DRES.EXTERNAL_ROOT.resolve(
//                        this.path
//                            ?: throw IllegalStateException("DbHint of type VIDEO is expected to hold a valid media item or external path but it doesn't! This is a programmer's error!")
//                    )
//                    this@GetTaskHintHandler.cache.asyncPreviewVideo(source, start, end).get()
//                }
//                if (Files.exists(path)) {
//                    Base64.getEncoder().encodeToString(Files.readAllBytes(path))
//                } else {
//                    null
//                }
//            }
//
//            DbHintType.TEXT -> this.text
//                ?: throw IllegalStateException("A hint of type  ${this.type.description} must have a valid text.")
//
//            DbHintType.EMPTY -> ""
//            else -> throw IllegalStateException("The hint type ${this.type.description} is not supported.")
//        }
//
//        val contentType = when (this.type) {
//            DbHintType.IMAGE -> ApiContentType.IMAGE
//            DbHintType.VIDEO -> ApiContentType.VIDEO
//            DbHintType.TEXT -> ApiContentType.TEXT
//            DbHintType.EMPTY -> ApiContentType.EMPTY
//            else -> throw IllegalStateException("The hint type ${this.type.description} is not supported.")
//        }
//
//        return ApiContentElement(contentType = contentType, content = content, offset = this.start ?: 0L)
//    }

    private fun ApiHint.toContentElement(): ApiContentElement {

        //TODO find a better place for this lookup
        val item = this.mediaItem?.let {itemId ->
            DbMediaItem.filter { it.mediaItemId eq itemId }.firstOrNull()
        }
        val range = if (item?.fps != null) {
            this.range?.toTemporalRange(item.fps!!)
        } else {
            null
        }

        val content = when (this.type) {
            ApiHintType.EMPTY -> ""
            ApiHintType.TEXT -> this.description
            ApiHintType.VIDEO -> {
                val start = range?.start?.toMilliseconds()
                    ?: throw IllegalStateException("ApiHint of type VIDEO is expected to hold a valid start timestamp but doesn't! This is a programmer's error!")
                val end = range.end.toMilliseconds()
                val path = if (item != null) {
                    this@GetTaskHintHandler.cache.asyncPreviewVideo(item, start, end)
                        .get() /* This should return immediately, since the previews have been prepared. */
                } else {
                    val source = DRES.EXTERNAL_ROOT.resolve(
                        this.path
                            ?: throw IllegalStateException("ApiHint of type VIDEO is expected to hold a valid media item or external path but it doesn't! This is a programmer's error!")
                    )
                    this@GetTaskHintHandler.cache.asyncPreviewVideo(source, start, end).get()
                }
                if (Files.exists(path)) {
                    Base64.getEncoder().encodeToString(Files.readAllBytes(path))
                } else {
                    null
                }
            }
            ApiHintType.IMAGE -> {
                val path = if (item != null) {
                    this@GetTaskHintHandler.cache.asyncPreviewImage(item)
                        .get() /* This should return immediately, since the previews have been prepared. */
                } else {
                    this@GetTaskHintHandler.cache.asyncPreviewImage(
                        DRES.EXTERNAL_ROOT.resolve(
                            this.path
                                ?: throw IllegalStateException("ApiHint of type IMAGE is expected to hold a valid media item or external path but it doesn't! This is a programmer's error!")
                        )
                    ).get()
                }
                if (Files.exists(path)) {
                    if (path.toString().endsWith(".jpg", ignoreCase = true)) {
                        Base64.getEncoder().encodeToString(Files.readAllBytes(path))
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

        return ApiContentElement(contentType = contentType, content = content, offset = this.start ?: 0L)
    }
}

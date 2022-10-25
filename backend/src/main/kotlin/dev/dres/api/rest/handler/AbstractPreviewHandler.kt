package dev.dres.api.rest.handler

import dev.dres.api.rest.RestApi
import dev.dres.api.rest.RestApiRole
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.dbo.DAO
import dev.dres.data.dbo.DaoIndexer
import dev.dres.data.model.Config
import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaCollection
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.submissions.aspects.ItemAspect
import dev.dres.data.model.submissions.aspects.TemporalSubmissionAspect
import dev.dres.data.model.submissions.aspects.TextAspect
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunExecutor
import dev.dres.utilities.FFmpegUtil
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.errorResponse
import dev.dres.utilities.extensions.sendFile
import dev.dres.utilities.extensions.streamFile
import io.javalin.http.Context
import io.javalin.openapi.*
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

abstract class AbstractPreviewHandler(
    private val collections: DAO<MediaCollection>,
    private val itemIndex: DaoIndexer<MediaItem, Pair<UID, String>>,
    config: Config
) : GetRestHandler<Any>, AccessManagedRestHandler {
    override val apiVersion = "v1"
    override val permittedRoles = setOf(RestApiRole.VIEWER)
    private val cacheLocation = Paths.get(config.cachePath + "/previews")

//    private val waitingMap = ConcurrentHashMap<Path, Long>()
//    private val timeOut = 10_000

    protected fun handlePreviewRequest(collectionId: UID, itemName: String, time: Long?, ctx: Context) {

        val item = itemIndex[collectionId to itemName].firstOrNull()
        if (item == null) {
            ctx.errorResponse(404, "Media item $itemName (collection = $collectionId) not found!")
            return
        }

        handlePreviewRequest(item, time, ctx)

    }

    private val missingImage = this.javaClass.getResourceAsStream("/img/missing.png")!!.readAllBytes()
    private val waitingImage = this.javaClass.getResourceAsStream("/img/loading.png")!!.readAllBytes()

    protected fun handlePreviewRequest(item: MediaItem, time: Long?, ctx: Context) {

        val collection = this.collections[item.collection]
            ?: throw ErrorStatusException(404, "Collection ${item.collection} does not exist.", ctx)

        val basePath = File(collection.path)


        if (item is MediaItem.ImageItem) {
            //TODO scale down image if too large
            ctx.header("Cache-Control", "max-age=31622400")
            ctx.streamFile(File(basePath, item.location))
            return
        } else if (item is MediaItem.VideoItem) {

            /* Prepare cache directory for item. */
            val cacheDir = cacheLocation.resolve("${item.collection}/${item.name}")
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir)
            }

            /* check timestamp. */
            if (time == null) {
                throw ErrorStatusException(400, "Timestamp unspecified or invalid.", ctx)
            }


            val imgPath = cacheDir.resolve("${time}.jpg")

            if (Files.exists(imgPath)) { //if file is available, send contents immediately
                ctx.header("Cache-Control", "max-age=31622400")
                ctx.sendFile(imgPath.toFile())
            } else { //if not, wait for it if necessary

                val future = FFmpegUtil.executeFFmpegAsync(Path.of(collection.path, item.location), time, imgPath)

                val waitTime = if (RestApi.readyThreadCount > 500) {
                    3L
                } else {
                    1L
                }

                try {
                    val path = future.get(waitTime, TimeUnit.SECONDS) ?: throw FileNotFoundException()
                    ctx.sendFile(path.toFile())
                } catch (e: TimeoutException) {
                    ctx.status(408)
                    ctx.header("Cache-Control", "max-age=30")
                    ctx.contentType("image/png")
                    ctx.result(waitingImage)
                } catch (t: Throwable) {
                    ctx.status(429)
                    ctx.header("Cache-Control", "max-age=600")
                    ctx.contentType("image/png")
                    ctx.result(missingImage)
                }
            }
        }
    }

    init {
        Files.createDirectories(this.cacheLocation)
    }

}

class MediaPreviewHandler(
    collections: DAO<MediaCollection>,
    itemIndex: DaoIndexer<MediaItem, Pair<UID, String>>,
    config: Config
) : AbstractPreviewHandler(collections, itemIndex, config) {

    @OpenApi(
        summary = "Returns a preview image from a collection item",
        path = "/api/v1/preview/item/{collection}/{item}/{time}",
        pathParams = [
            OpenApiParam("collectionId", String::class, "Unique ID of the collection."),
            OpenApiParam("item", String::class, "Name of the MediaItem"),
            OpenApiParam("time", Long::class, "Time into the video in milliseconds (for videos only).")
        ],
        tags = ["Media"],
        responses = [OpenApiResponse(
            "200",
            [OpenApiContent(type = "image/png")]
        ), OpenApiResponse("401"), OpenApiResponse("400")],
        ignore = true,
        methods = [HttpMethod.GET]
    )
    override fun get(ctx: Context) {

        try {
            val params = ctx.pathParamMap()

            val collectionId = params["collection"]?.UID()
                ?: throw ErrorStatusException(400, "Collection ID not specified or invalid.", ctx)
            val itemName = params["item"] ?: throw ErrorStatusException(400, "Item name not specified.", ctx)
            val time = params["time"]?.toLongOrNull()

            handlePreviewRequest(collectionId, itemName, time, ctx)
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        }

    }


    override val route: String = "preview/item/{collection}/{item}/{time}"

    //not used
    override fun doGet(ctx: Context): Any = ""

}


class SubmissionPreviewHandler(
    collections: DAO<MediaCollection>,
    itemIndex: DaoIndexer<MediaItem, Pair<UID, String>>,
    config: Config
) : AbstractPreviewHandler(collections, itemIndex, config) {

    @OpenApi(
        summary = "Returns a preview image for a submission",
        path = "/api/v1/preview/submission/{runId}/{submissionId}",
        pathParams = [
            OpenApiParam("runId", String::class, "Competition Run ID"),
            OpenApiParam("submissionId", String::class, "Subission ID")
        ],
        tags = ["Media"],
        responses = [OpenApiResponse(
            "200",
            [OpenApiContent(type = "image/png")]
        ), OpenApiResponse("401"), OpenApiResponse("400")],
        ignore = true,
        methods = [HttpMethod.GET]
    )
    override fun get(ctx: Context) {

        try {
            val params = ctx.pathParamMap()

            val runId = params["runId"]?.UID()
                ?: throw ErrorStatusException(404, "Parameter 'runId' is invalid", ctx)

            val submissionId = params["submissionId"]?.UID()
                ?: throw ErrorStatusException(404, "Parameter 'submissionId' is missing", ctx)

            val run = RunExecutor.managerForId(runId)
                ?: throw ErrorStatusException(404, "Competition Run $runId not found", ctx)

            if (run !is InteractiveRunManager) {
                throw ErrorStatusException(404, "Competition Run $runId is not interactive", ctx)
            }

            val submission = run.allSubmissions.find { it.uid == submissionId }
                ?: throw ErrorStatusException(404, "Submission '$submissionId' not found", ctx)

            when (submission) {
                is ItemAspect -> {
                    handlePreviewRequest(
                        submission.item,
                        if (submission is TemporalSubmissionAspect) submission.start else null, ctx
                    )
                }
                is TextAspect -> {
                    ctx.header("Cache-Control", "max-age=31622400")
                    ctx.contentType("image/png")
                    ctx.result(this.javaClass.getResourceAsStream("/img/text.png")!!)
                }
                else -> {
                    ctx.header("Cache-Control", "max-age=31622400")
                    ctx.contentType("image/png")
                    ctx.result(this.javaClass.getResourceAsStream("/img/missing.png")!!)
                }
            }
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        }

    }

    override val route: String = "preview/submission/{runId}/{submissionId}"

    //not used
    override fun doGet(ctx: Context): Any = ""

}

package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatusException
import dres.data.dbo.DAO
import dres.data.dbo.DaoIndexer
import dres.data.model.Config
import dres.data.model.basics.media.MediaCollection
import dres.data.model.basics.media.MediaItem
import dres.run.RunExecutor
import dres.utilities.FFmpegUtil
import dres.utilities.extensions.errorResponse
import dres.utilities.extensions.streamFile
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

abstract class AbstractPreviewHandler(private val collections: DAO<MediaCollection>, items: DAO<MediaItem>, config: Config) : GetRestHandler<Any>, AccessManagedRestHandler {

    override val permittedRoles = setOf(RestApiRole.VIEWER)
    private val cacheLocation = Paths.get(config.cachePath + "/previews")
    private val itemIndex = DaoIndexer(items) { it.collection to it.name }

    protected fun handlePreviewRequest(collectionId: Long, itemName: String, time: Long?, ctx: Context) {

        val item = itemIndex[collectionId to itemName].firstOrNull()
        if (item == null) {
            ctx.errorResponse(404, "Media item $itemName (collection = $collectionId) not found!")
            return
        }

        handlePreviewRequest(item, time, ctx)

    }


    protected fun handlePreviewRequest(item: MediaItem, time: Long?, ctx: Context) {

        val collection = this.collections[item.collection]
                ?: throw ErrorStatusException(404, "Collection ${item.collection} does not exist.")

        val basePath = File(collection.basePath)


        if (item is MediaItem.ImageItem) {
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
                throw ErrorStatusException(400, "Timestamp unspecified or invalid.")
            }


            val imgFile = cacheDir.resolve("${time}.jpg")
            if (!Files.exists(imgFile)) {
                val mediaItemLocation = Path.of(collection.basePath, item.location)
                //sanity check
                if (time < 0 || time > item.durationMs || !Files.exists(mediaItemLocation)) {
                    imgFile.toFile().writeText("missing")
                } else {
                    imgFile.toFile().createNewFile() //create empty file as placeholder
                    FFmpegUtil.extractFrame(mediaItemLocation, time, imgFile)
                }
            }


            var tryCounter = 0

            //check if file is empty wait //TODO better solution
            while (imgFile.toFile().length() == 0L && tryCounter++ < 300) {
                Thread.sleep(100)
            }

            //return placeholder for invalid files
            if (imgFile.toFile().length() < 100) {
                ctx.contentType("image/png")
                ctx.result(this.javaClass.getResourceAsStream("/img/missing.png"))
            } else {
                ctx.streamFile(imgFile)
            }
        }
    }

    init {
        Files.createDirectories(this.cacheLocation)
    }

}

class MediaPreviewHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>, config: Config) : AbstractPreviewHandler(collections, items, config) {

    @OpenApi(summary = "Returns a preview image from a collection item",
            path = "/api/preview/item/:collection/:item/:time",
            pathParams = [
                OpenApiParam("collectionId", Long::class, "Unique ID of the collection."),
                OpenApiParam("item", String::class, "Name of the MediaItem"),
                OpenApiParam("time", Long::class, "Time into the video in milliseconds (for videos only).")
            ],
            tags = ["Media"],
            responses = [OpenApiResponse("200", [OpenApiContent(type = "image/png")]), OpenApiResponse("401"), OpenApiResponse("400")],
            ignore = true
    )
    override fun get(ctx: Context) {

        val params = ctx.pathParamMap()

        val collectionId = params["collection"]?.toLongOrNull()
                ?: throw ErrorStatusException(400, "Collection ID not specified or invalid.")
        val itemName = params["item"] ?: throw ErrorStatusException(400, "Item name not specified.")
        val time = params["time"]?.toLongOrNull()

        handlePreviewRequest(collectionId, itemName, time, ctx)

    }


    override val route: String = "preview/item/:collection/:item/:time"

    //not used
    override fun doGet(ctx: Context): Any = ""

}


class SubmissionPreviewHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>, config: Config) : AbstractPreviewHandler(collections, items, config) {

    @OpenApi(summary = "Returns a preview image for a submission",
            path = "/api/preview/submission/:runId/:submissionId",
            pathParams = [
                OpenApiParam("runId", Long::class, "Competition Run ID"),
                OpenApiParam("submissionId", String::class, "Subission ID")
            ],
            tags = ["Media"],
            responses = [OpenApiResponse("200", [OpenApiContent(type = "image/png")]), OpenApiResponse("401"), OpenApiResponse("400")],
            ignore = true
    )
    override fun get(ctx: Context) {

        val params = ctx.pathParamMap()

        val runId = params["runId"]?.toLongOrNull() ?: throw ErrorStatusException(404, "Parameter 'runId' is invalid")
        val submissionId = params["submissionId"] ?: throw ErrorStatusException(404, "Parameter 'submissionId' is missing")

        val run = RunExecutor.managerForId(runId) ?: throw ErrorStatusException(404, "Competition Run $runId not found")

        val submission = run.allSubmissions.find { it.id == submissionId } ?: throw ErrorStatusException(404, "Submission '$submissionId' not found")

        handlePreviewRequest(submission.item, submission.start, ctx)

    }

    override val route: String = "preview/submission/:runId/:submissionId"

    //not used
    override fun doGet(ctx: Context): Any = ""

}
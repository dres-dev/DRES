package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatusException
import dres.data.dbo.DAO
import dres.data.model.basics.media.MediaCollection
import dres.data.model.basics.media.MediaItem
import dres.utilities.FFmpegUtil
import dres.utilities.TimeUtil
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

class GetPreviewHandler(private val collections: DAO<MediaCollection>, private val items: DAO<MediaItem>) : GetRestHandler<Any>, AccessManagedRestHandler {

    companion object {
        private val cacheLocation = Paths.get("cache") //TODO make configurable
        private const val imageMime = "image/png"

        init {
            Files.createDirectories(this.cacheLocation)
        }
    }

    @OpenApi(summary = "Returns a preview image from a collection item",
            path = "/api/preview/:collection/:item/:time",
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


        val collectionId = params["collection"]?.toLongOrNull() ?: throw ErrorStatusException(400, "Collection ID not specified or invalid.")
        val itemName = params["item"] ?: throw ErrorStatusException(400, "Item name not specified.")
        val collection = this.collections[collectionId] ?: throw ErrorStatusException(404, "Collection $collectionId does not exist.")
        val item = items.find { it.collection == collectionId && it.name == itemName }
        if (item == null){
            ctx.errorResponse(404, "Media item $itemName (collection = $collectionId) not found!")
            return
        }

        val basePath = File(collection.basePath)

        if (item is MediaItem.ImageItem) {
            ctx.streamFile(File(basePath, item.location))
            return
        } else if (item is MediaItem.VideoItem){
            /* Prepare cache directory for item. */
            val cacheDir = cacheLocation.resolve("${params["collection"]}/${params["item"]}")
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir)
            }

            /* Extract timestamp. */
            val time = params["time"]?.toLongOrNull() ?: throw ErrorStatusException(400, "Timestamp unspecified or invalid.")
            val imgFile = cacheDir.resolve("${time}.png")
            if (!Files.exists(imgFile)){
                FFmpegUtil.extractFrame(Path.of(collection.basePath, item.location), time, imgFile)
            }

            ctx.streamFile(imgFile)
        }

    }

    override val permittedRoles = setOf(RestApiRole.VIEWER)
    override val route: String = "preview/:collection/:item/:time"

    //not used
    override fun doGet(ctx: Context): Any = ""

}
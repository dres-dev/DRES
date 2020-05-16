package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatusException
import dres.data.dbo.DAO
import dres.data.dbo.DaoIndexer
import dres.data.model.Config
import dres.data.model.basics.media.MediaCollection
import dres.data.model.basics.media.MediaItem
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

class GetPreviewHandler(private val collections: DAO<MediaCollection>, items: DAO<MediaItem>, config: Config) : GetRestHandler<Any>, AccessManagedRestHandler {


    private val cacheLocation = Paths.get(config.cachePath + "/previews")
    private val imageMime = "image/png"

    init {
        Files.createDirectories(this.cacheLocation)
    }


    private val itemIndex = DaoIndexer(items){it.collection to it.name}

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
        val item = itemIndex[collectionId to itemName].firstOrNull()
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
                imgFile.toFile().createNewFile() //create empty file as placeholder
                FFmpegUtil.extractFrame(Path.of(collection.basePath, item.location), time, imgFile)
            }

            //check if file is empty and return placeholder
            if (imgFile.toFile().length() == 0L) {
                //set header to not cache
                ctx.header("Cache-Control", "no-cache, no-store, must-revalidate")
                //return placeholder
                ctx.contentType(imageMime)
                ctx.result(
                        this.javaClass.getResourceAsStream("/img/loading.png")
                )
            } else {
                ctx.header("Cache-Control", "public, max-age=31536000")
                ctx.streamFile(imgFile)
            }




        }

    }

    override val permittedRoles = setOf(RestApiRole.VIEWER)
    override val route: String = "preview/:collection/:item/:time"

    //not used
    override fun doGet(ctx: Context): Any = ""

}
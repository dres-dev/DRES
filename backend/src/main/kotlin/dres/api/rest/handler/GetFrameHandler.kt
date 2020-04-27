package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.data.dbo.DAO
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
import java.nio.file.Path

class GetFrameHandler(private val collections: DAO<MediaCollection>, private val items: DAO<MediaItem>) : GetRestHandler<Any>, AccessManagedRestHandler {

    companion object {
        private val cacheLocation = File("cache") //TODO make configurable
        private const val imageMime = "image/png"

        init {
            cacheLocation.mkdirs()
        }

    }

    @OpenApi(summary = "returns a image from a collection item",
            path = "/api/frame/:collection/:item/:time",
            pathParams = [
                OpenApiParam("collection", String::class, "Collection name"),
                OpenApiParam("item", String::class, "MediaItem name"),
                OpenApiParam("time", String::class, "time code")
            ],
            tags = ["Media"],
            responses = [OpenApiResponse("200", [OpenApiContent(type = "image/png")]), OpenApiResponse("401"), OpenApiResponse("400")],
            ignore = true
            )
    override fun get(ctx: Context) {

        val params = ctx.pathParamMap()

        if (!params.containsKey("collection") || !params.containsKey("item")){
            ctx.errorResponse(400, "missing parameters")
            return
        }

        val collection = collections.find { it.name == params["collection"] }
        
        if (collection == null){
            ctx.errorResponse(404, "collection not found")
            return
        }
        
        val item = items.find { it.collection == collection.id && it.name == params["item"] }

        if (item == null){
            ctx.errorResponse(404, "item not found")
            return
        }

        if (item is MediaItem.ImageItem) {
            ctx.streamFile(File(item.location))
            return
        } else if (item is MediaItem.VideoItem){

            if (!params.containsKey("time")){
                ctx.errorResponse(400, "missing parameters")
                return
            }

            val cacheDir = File(cacheLocation, "${params["collection"]}/${params["item"]}")
            cacheDir.mkdirs()

            val time = params["time"]!! //TODO sanitize

            val imgFile = File(cacheDir, "${time}.png")

            if (!imgFile.exists()){
                FFmpegUtil.extractFrame(Path.of(item.location), time, imgFile.toPath())
            }

            ctx.streamFile(imgFile)
        }

    }

    override val permittedRoles = setOf(RestApiRole.VIEWER)
    override val route: String = "frame/:collection/:item/:time"

    //not used
    override fun doGet(ctx: Context): Any = ""


}
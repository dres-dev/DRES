package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.util.StaticFileHelper
import dres.data.dbo.DAO
import dres.data.model.basics.MediaCollection
import dres.data.model.basics.MediaItem
import dres.utilities.FFmpegUtil

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import java.io.File

class GetFrameHandler(private val collections: DAO<MediaCollection>, private val items: DAO<MediaItem>) : GetRestHandler, AccessManagedRestHandler {

    companion object {
        private val cacheLocation = File("cache") //TODO make configurable

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
            responses = [OpenApiResponse("200", [OpenApiContent(type = "image/png")]), OpenApiResponse("401")]
            )
    override fun get(ctx: Context) {

        val params = ctx.pathParamMap()

        if (!params.containsKey("collection") || !params.containsKey("item")){
            ctx.status(400).result("missing parameters")
            return
        }

        val collection = collections.find { it.name == params["collection"] }
        
        if (collection == null){
            ctx.status(404).result("collection not found")
            return
        }
        
        val item = items.find { it.collection == collection.id && it.name == params["item"] }

        if (item == null){
            ctx.status(404).result("item not found")
            return
        }

        if (item is MediaItem.ImageItem) {
            StaticFileHelper.serveFile(ctx, item.location.toFile())
        } else if (item is MediaItem.VideoItem){

            if (!params.containsKey("time")){
                ctx.status(400).result("missing parameters")
                return
            }

            val cacheDir = File(cacheLocation, "${params["collection"]}/${params["item"]}")
            cacheDir.mkdirs()

            val time = params["time"]!! //TODO sanitize

            val imgFile = File(cacheDir, "${time}.png")

            if (!imgFile.exists()){

                FFmpegUtil.extractFrame(item.location, time, imgFile.toPath())

            }

            StaticFileHelper.serveFile(ctx, imgFile)

        }

    }

    override val permittedRoles = setOf(RestApiRole.VIEWER)
    override val route: String = "frame/:collection/:item/:time"



}
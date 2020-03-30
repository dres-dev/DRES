package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.data.dbo.DAO
import dres.data.model.basics.MediaCollection
import dres.data.model.basics.MediaItem
import dres.utilities.extensions.errorResponse
import dres.utilities.extensions.streamFile
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import java.io.File

class GetMediaHandler(private val collections: DAO<MediaCollection>, private val items: DAO<MediaItem>) : GetRestHandler<Any>, AccessManagedRestHandler {

    override val permittedRoles = setOf(RestApiRole.VIEWER)
    override val route: String = "media/:collection/:item"

    //not used
    override fun doGet(ctx: Context): Any = ""

    @OpenApi(summary = "Returns a collection item",
            path = "/api/media/:collection/:item",
            pathParams = [
                OpenApiParam("collection", String::class, "Collection name"),
                OpenApiParam("item", String::class, "MediaItem name")
            ],
            tags = ["Media"],
            responses = [OpenApiResponse("200"), OpenApiResponse("401"), OpenApiResponse("400"), OpenApiResponse("404")]
    )
    override fun get(ctx: Context) {

        val params = ctx.pathParamMap()

        if (!params.containsKey("collection") || !params.containsKey("item")) {
            ctx.errorResponse(400, "missing parameters")
            return
        }

        val collectionName = params["collection"]!!
        val collection = collections.find { it.name == collectionName }

        if (collection == null) {
            ctx.errorResponse(404, "collection not found")
            return
        }

        val itemName = params["item"]!!
        val item = items.find { it.collection == collection.id && it.name == itemName }

        if (item == null) {
            ctx.errorResponse(404, "item with name $itemName found")
            return
        }

        val basePath = File(collection.basePath)
        val itemFile = File(basePath, item.location)

        ctx.streamFile(itemFile)

    }

}
package dev.dres.api.rest.handler

import dev.dres.api.rest.RestApiRole
import dev.dres.data.dbo.DAO
import dev.dres.data.dbo.DaoIndexer
import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaCollection
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.errorResponse
import dev.dres.utilities.extensions.streamFile
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import java.io.File

class GetMediaHandler(private val collections: DAO<MediaCollection>, private val itemCache: DaoIndexer<MediaItem, Pair<UID,UID>>, private val collectionCache : DaoIndexer<MediaCollection, UID>) : GetRestHandler<Any>, AccessManagedRestHandler {

    override val permittedRoles = setOf(RestApiRole.VIEWER)
    override val route: String = "media/:collection/:item"

    //not used
    override fun doGet(ctx: Context): Any = ""

    @OpenApi(summary = "Returns a collection item",
            path = "/api/media/:collection/:item",
            pathParams = [
                OpenApiParam("collection", String::class, "Collection id"),
                OpenApiParam("item", String::class, "MediaItem id")
            ],
            tags = ["Media"],
            responses = [OpenApiResponse("200"), OpenApiResponse("401"), OpenApiResponse("400"), OpenApiResponse("404")],
            ignore = true
    )
    override fun get(ctx: Context) {

        val params = ctx.pathParamMap()

        if (!params.containsKey("collection") || !params.containsKey("item")) {
            ctx.errorResponse(400, "missing parameters")
            return
        }

        val collectionUid = params["collection"]!!.UID()
        val collection = collectionCache[collectionUid].firstOrNull() //collections.find { it.name == collectionName }

        if (collection == null) {
            ctx.errorResponse(404, "collection not found")
            return
        }

        val itemUid = params["item"]!!.UID()
        val item = itemCache[collection.id to itemUid].firstOrNull()//items.find { it.collection == collection.id && it.name == itemName }

        if (item == null) {
            ctx.errorResponse(404, "item with name $itemUid found")
            return
        }

        val basePath = File(collection.basePath)
        val itemFile = File(basePath, item.location)

        ctx.streamFile(itemFile)

    }

}
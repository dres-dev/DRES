package dev.dres.api.rest.handler.preview

import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.Config
import dev.dres.data.model.media.DbMediaItem
import dev.dres.utilities.extensions.errorResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * An [AbstractPreviewHandler] used to access previews of specific [DbMediaItem]s.
 *
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class MediaPreviewHandler(store: TransientEntityStore, config: Config) : AbstractPreviewHandler(store, config) {

    override val route: String = "preview/item/{collectionId}/{item}/{time}"
    @OpenApi(
        summary = "Returns a preview image from a collection item",
        path = "/api/v2/preview/item/{collectionId}/{item}/{time}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [
            OpenApiParam("collectionId", String::class, "Unique ID of the media collection."),
            OpenApiParam("item", String::class, "Name of the media item-"),
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
            val collectionId = params["collection"] ?: throw ErrorStatusException(400, "Collection ID not specified or invalid.", ctx)
            val itemName = params["item"] ?: throw ErrorStatusException(400, "Item name not specified.", ctx)
            val time = params["time"]?.toLongOrNull()
            handlePreviewRequest(collectionId, itemName, time, ctx)
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        }
    }

    override fun doGet(ctx: Context): Any {
        throw UnsupportedOperationException("MediaPreviewHandler::doGet() is not supported and should not be executed!")
    }
}

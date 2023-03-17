package dev.dres.api.rest.handler.preview

import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.media.DbMediaItem
import dev.dres.mgmt.cache.CacheManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class PreviewImageHandler(store: TransientEntityStore, cache: CacheManager) : AbstractPreviewHandler(store, cache) {

    override val route: String = "preview/{mediaItemId}/{time}"
    @OpenApi(
        summary = "Returns a preview image from a media item.",
        path = "/api/v2/preview/{mediaItemId}/{time}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [
            OpenApiParam("mediaItemId", String::class, "Unique ID of the media collection.", required = true, allowEmptyValue = false),
            OpenApiParam("timestamp", Long::class, "Time into the video in milliseconds (for videos only).", required = false, allowEmptyValue = false)
        ],
        tags = ["Media"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(type = "image/jpeg")]),
            OpenApiResponse("202"),
            OpenApiResponse("401"),
            OpenApiResponse("404"),
            OpenApiResponse("400")
        ],
        ignore = true,
        methods = [HttpMethod.GET]
    )
    override fun get(ctx: Context) {
        val params = ctx.pathParamMap()
        val mediaItemId = params["mediaItemId"] ?: throw ErrorStatusException(400, "Media item ID not specified or invalid.", ctx)
        val time = params["timestamp"]?.toLongOrNull()
        this.store.transactional(true) {
            val item = DbMediaItem.query(DbMediaItem::id eq mediaItemId).firstOrNull() ?: throw ErrorStatusException(404, "Could not find media item with ID ${mediaItemId}.", ctx)
            handlePreviewImageRequest(item, time, ctx)
        }
    }

    override fun doGet(ctx: Context): Any {
        throw UnsupportedOperationException("PreviewImageHandler::doGet() is not supported and should not be executed!")
    }
}
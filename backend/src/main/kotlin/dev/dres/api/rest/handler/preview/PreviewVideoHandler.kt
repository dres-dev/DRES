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
 * A general purpose, [AbstractPreviewHandler] that handles video previews for different [DbMediaItem].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class PreviewVideoHandler(store: TransientEntityStore, cache: CacheManager) : AbstractPreviewHandler(store, cache) {

    override val route: String = "preview/{mediaItemId}/{time}"
    @OpenApi(
        summary = "Returns a preview video from a media item. ",
        path = "/api/v2/preview/{mediaItemId}/{start}/{end}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [
            OpenApiParam("mediaItemId", String::class, "Unique ID of the media item. Must be a video.", required = true, allowEmptyValue = false),
            OpenApiParam("start", Long::class, "Start timestamp into the video in milliseconds.", required = true, allowEmptyValue = false),
            OpenApiParam("end", Long::class, "End timestamp into the video in milliseconds.", required = true, allowEmptyValue = false)
        ],
        tags = ["Media"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(type = "video/mp4")]),
            OpenApiResponse("202"),
            OpenApiResponse("400"),
            OpenApiResponse("404")
        ],
        methods = [HttpMethod.GET]
    )
    override fun get(ctx: Context) {
        val params = ctx.pathParamMap()
        val mediaItemId = params["mediaItemId"] ?: throw ErrorStatusException(400, "Media item ID not specified or invalid.", ctx)
        val start = params["start"]?.toLongOrNull() ?: throw ErrorStatusException(400, "Start timestamp must be specified.", ctx)
        val end = params["end"]?.toLongOrNull() ?: throw ErrorStatusException(400, "End timestamp must be specified.", ctx)
        this.store.transactional(true) {
            val item = DbMediaItem.query(DbMediaItem::id eq mediaItemId).firstOrNull() ?: throw ErrorStatusException(404, "Could not find media item with ID ${mediaItemId}.", ctx)
            handlePreviewVideoRequest(item, start, end, ctx)
        }
    }

    override fun doGet(ctx: Context): Any {
        throw UnsupportedOperationException("PreviewImageHandler::doGet() is not supported and should not be executed!")
    }
}

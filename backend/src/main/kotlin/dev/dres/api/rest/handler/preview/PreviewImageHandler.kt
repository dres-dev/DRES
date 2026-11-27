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
 * A general purpose, [AbstractPreviewHandler] that handles image previews for different [DbMediaItem].
 *
 * [PreviewImageHandler] vs [PreviewImageTimelessHandler]: Optional path parameters are not allowed in OpenApi.
 *
 * @author Ralph Gasser and Loris Sauter
 * @version 1.1.0
 */
class PreviewImageHandler(store: TransientEntityStore, cache: CacheManager) : AbstractPreviewHandler(store, cache) {

    override val route: String = "preview/{mediaItemId}/{timestamp}"
    @OpenApi(
        summary = "Returns a preview image from a media item.",
        path = "/api/v2/preview/{mediaItemId}/{timestamp}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [
            OpenApiParam("mediaItemId", String::class, "Unique ID of the media item.", required = true, allowEmptyValue = false),
            OpenApiParam("timestamp", Long::class, "Time into the video in milliseconds (for videos only).", required = true, allowEmptyValue = false)
        ],
        tags = ["Media"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(type = "image/jpeg")]),
            OpenApiResponse("202"),
            OpenApiResponse("400"),
            OpenApiResponse("404")
        ],
        methods = [HttpMethod.GET]
    )
    override fun get(ctx: Context) {
        val params = ctx.pathParamMap()
        val mediaItemId = params["mediaItemId"] ?: throw ErrorStatusException(400, "Media item ID was not specified or is invalid.", ctx)
        /*
         * A missing or unparseable timestamp used to become 0, silently serving the frame at the start of
         * the video for a typo instead of telling the caller. Only an absent segment means "the beginning",
         * which is the timeless handler's job, not this one's.
         */
        val time = params["timestamp"]?.toLongOrNull()
            ?: throw ErrorStatusException(400, "Timestamp must be a whole number of milliseconds.", ctx)
        this.store.transactional(true) {
            val item = DbMediaItem.query(DbMediaItem::id eq mediaItemId).firstOrNull()
            handlePreviewImageRequest(item, time, ctx)
        }
    }

    override fun doGet(ctx: Context): Any {
        throw UnsupportedOperationException("PreviewImageHandler::doGet() is not supported and should not be executed!")
    }
}

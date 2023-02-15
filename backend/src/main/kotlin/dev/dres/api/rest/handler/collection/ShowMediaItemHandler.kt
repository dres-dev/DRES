package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.media.DbMediaItem
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ShowMediaItemHandler(store: TransientEntityStore) : AbstractCollectionHandler(store), GetRestHandler<ApiMediaItem> {
    override val route: String = "mediaItem/{mediaItemId}"

    @OpenApi(
        summary = "Selects and returns a specific media item.",
        path = "/api/v2/mediaItem/{mediaItemId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam("mediaItemId", String::class, "Media item ID.", required = true, allowEmptyValue = false)
        ],
        tags = ["Collection"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiMediaItem::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): ApiMediaItem {
        val mediaId = ctx.pathParamMap().getOrElse("mediaItemId") {
            throw ErrorStatusException(404, "Parameter 'mediaItemId' is missing!'", ctx)
        }

        return this.store.transactional(true) {
            val item = DbMediaItem.query(DbMediaItem::id eq mediaId).firstOrNull()
                ?: throw ErrorStatusException(404, "Media item with ID $mediaId not found.", ctx)
            item.toApi()
        }
    }
}

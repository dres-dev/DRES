package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.DeleteRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.basics.media.MediaItem
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
class DeleteMediaItemHandler(store: TransientEntityStore) : AbstractCollectionHandler(store), DeleteRestHandler<SuccessStatus> {

    override val route: String = "mediaItem/{mediaId}"

    @OpenApi(
        summary = "Tries to delete a specific media item.",
        path = "/api/v1/mediaItem/{mediaId}",
        methods = [HttpMethod.DELETE],
        pathParams = [
            OpenApiParam("mediaId", String::class, "Media item ID")
        ],
        tags = ["Collection"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doDelete(ctx: Context): SuccessStatus {
        val mediaId = ctx.pathParamMap().getOrElse("mediaId") {
            throw ErrorStatusException(404, "Parameter 'mediaId' is missing!'", ctx)
        }

        return this.store.transactional {
            val item = MediaItem.query(MediaItem::id eq mediaId).firstOrNull()
                ?: throw ErrorStatusException(404, "Media item with ID $mediaId not found.", ctx)
            item.delete()
            SuccessStatus("Media item ${item.id} deleted successfully.")
        }
    }
}
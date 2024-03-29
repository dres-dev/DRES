package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.PatchRestHandler
import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.media.DbMediaItem
import dev.dres.mgmt.MediaCollectionManager
import io.javalin.http.BadRequestResponse
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
class UpdateMediaItemHandler : AbstractCollectionHandler(), PatchRestHandler<SuccessStatus> {

    override val route: String = "mediaitem"

    @OpenApi(
        summary = "Updates a Media Item to the specified Media Collection.",
        path = "/api/v2/mediaitem",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.PATCH],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiMediaItem::class)]),
        tags = ["Collection"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPatch(ctx: Context): SuccessStatus {
        /* Parse media item and perform sanity checks */
        val mediaItem = try {
            ctx.bodyAsClass(ApiMediaItem::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        } catch (e: IllegalArgumentException) {
            throw ErrorStatusException(400, e.message ?: "Invalid parameters. This is a programmers error!", ctx)
        }

        try{
            MediaCollectionManager.updateMediaItem(mediaItem)
            return SuccessStatus("Media item ${mediaItem.mediaItemId} updated successfully.")
        } catch (e: Exception) {
            throw ErrorStatusException(404, e.message ?: "Could not update item", ctx)
        }
    }
}

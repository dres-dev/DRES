package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.media.DbMediaCollection
import dev.dres.data.model.media.DbMediaItem
import dev.dres.mgmt.MediaCollectionManager
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class AddMediaItemHandler : AbstractCollectionHandler(), PostRestHandler<SuccessStatus> {

    override val route: String = "mediaItem"

    @OpenApi(
        summary = "Adds a media item to the specified media collection.",
        path = "/api/v2/mediaItem",
        methods = [HttpMethod.POST],
        operationId = OpenApiOperation.AUTO_GENERATE,
        requestBody = OpenApiRequestBody([OpenApiContent(ApiMediaItem::class)]),
        tags = ["Collection"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus {

        /* Parse media item and perform sanity checks */
        val mediaItem = try {
            ctx.bodyAsClass(ApiMediaItem::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        } catch (e: IllegalArgumentException) {
            throw ErrorStatusException(400, e.message ?: "Invalid parameters. This is a programmers error!", ctx)
        }

        try {
            MediaCollectionManager.addMediaItem(mediaItem)
            return SuccessStatus("Media item added successfully.")
        } catch (e: Exception) {
            throw ErrorStatusException(400, e.message ?: "Could not create item", ctx)
        }

    }
}

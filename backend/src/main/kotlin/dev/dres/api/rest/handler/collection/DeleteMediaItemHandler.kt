package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.DeleteRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.mgmt.MediaCollectionManager
import io.javalin.http.Context
import io.javalin.openapi.*
import java.lang.Exception

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class DeleteMediaItemHandler : AbstractCollectionHandler(), DeleteRestHandler<SuccessStatus> {

    override val route: String = "mediaItem/{mediaId}"

    @OpenApi(
        summary = "Tries to delete a specific media item.",
        path = "/api/v2/mediaItem/{mediaId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.DELETE],
        pathParams = [
            OpenApiParam("mediaId", String::class, "Media item ID", required = true)
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

        try {
            MediaCollectionManager.deleteMediaItem(mediaId)
            return SuccessStatus("Media item $mediaId deleted successfully.")
        } catch (e: Exception) {
            throw ErrorStatusException(404, e.message ?: "Could not delete item", ctx)
        }
    }
}

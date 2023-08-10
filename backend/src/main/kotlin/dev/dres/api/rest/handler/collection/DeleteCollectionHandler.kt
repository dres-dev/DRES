package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.DeleteRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.mgmt.MediaCollectionManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class DeleteCollectionHandler : AbstractCollectionHandler(), DeleteRestHandler<SuccessStatus> {

    override val route: String = "collection/{collectionId}"

    @OpenApi(
        summary = "Deletes a media collection identified by a collection id.",
        path = "/api/v2/collection/{collectionId}",
        pathParams = [OpenApiParam("collectionId", String::class, "Collection ID", required = true)],
        tags = ["Collection"],
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.DELETE],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doDelete(ctx: Context): SuccessStatus {

        val collectionId = collectionId(ctx)

        val deleted = MediaCollectionManager.deleteCollection(collectionId)

        if (deleted != null) {
            return SuccessStatus("Collection $collectionId deleted successfully.")
        } else {
            throw ErrorStatusException(404, "Collection $collectionId not found", ctx)
        }
    }

}

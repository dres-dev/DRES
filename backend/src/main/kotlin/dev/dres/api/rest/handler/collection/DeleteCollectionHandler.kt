package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.DeleteRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.SuccessStatus
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class DeleteCollectionHandler(store: TransientEntityStore) : AbstractCollectionHandler(store), DeleteRestHandler<SuccessStatus> {

    @OpenApi(
        summary = "Deletes a media collection identified by a collection id.",
        path = "/api/v2/collection/{collectionId}",
        pathParams = [OpenApiParam("collectionId", String::class, "Collection ID")],
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
        val status = this.store.transactional {
            val collection = collectionFromContext(ctx)
            collection.delete()
            SuccessStatus("Collection ${collection.id} deleted successfully.")
        }
        return status
    }

    override val route: String = "collection/{collectionId}"

}

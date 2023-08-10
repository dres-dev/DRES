package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.collection.ApiPopulatedMediaCollection
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.mgmt.MediaCollectionManager
import io.javalin.http.Context
import io.javalin.openapi.*


/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ShowCollectionHandler() : AbstractCollectionHandler(), GetRestHandler<ApiPopulatedMediaCollection> {

    override val route: String = "collection/{collectionId}"

    @OpenApi(
        summary = "Shows the content of the specified media collection.",
        path = "/api/v2/collection/{collectionId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [OpenApiParam("collectionId", String::class, "Collection ID", required = true)],
        tags = ["Collection"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiPopulatedMediaCollection::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiPopulatedMediaCollection {
        val id = collectionId(ctx)
        return MediaCollectionManager.getPopulatedCollection(id) ?: throw ErrorStatusException(404, "Collection '$id' not found.'", ctx)
    }
}

package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.mgmt.MediaCollectionManager
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class ResolveMediaItemListByNameHandler : AbstractCollectionHandler(), PostRestHandler<List<ApiMediaItem>> {
    override val route = "collection/{collectionId}/resolve"

    @OpenApi(
        summary = "Resolves a list of media item names to media items",
        path = "/api/v2/collection/{collectionId}/resolve",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.POST],
        pathParams = [
            OpenApiParam("collectionId", String::class, "Collection ID", required = true)
        ],
        requestBody = OpenApiRequestBody([OpenApiContent(Array<String>::class)]),
        tags = ["Collection"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiMediaItem>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): List<ApiMediaItem> {
        val queriedNames = try {
            ctx.bodyAsClass(Array<String>::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters: Expected Array<String>. This is a programmers error!", ctx)
        }

        /** If no name is contained in the list, an empty result set will be returned.*/
        if (queriedNames.isEmpty()) {
            return emptyList()
        }

        /** Execute query. */
        return MediaCollectionManager.getMediaItemsByName(collectionId(ctx), queriedNames.toList())
    }

}

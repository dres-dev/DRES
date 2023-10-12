package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.collection.ApiMediaItem
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
class ListMediaItemHandler : AbstractCollectionHandler(), GetRestHandler<List<ApiMediaItem>> {
    override val route: String = "collection/{collectionId}/{startsWith}"

    @OpenApi(
        summary = "Lists media items from a given media collection whose name start with the given string.",
        path = "/api/v2/collection/{collectionId}/{startsWith}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam("collectionId", String::class, "Collection ID", required = true),
            OpenApiParam("startsWith", String::class, "Name the item(s) should start with.", required = true)
        ],
        tags = ["Collection"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiMediaItem>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): List<ApiMediaItem> {
        val collectionId = collectionId(ctx)
        val start = ctx.pathParamMap()["startsWith"]
        return MediaCollectionManager.getMediaItemsByPartialName(collectionId, start)
    }
}

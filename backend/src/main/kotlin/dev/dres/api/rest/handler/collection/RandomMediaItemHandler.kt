package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.mgmt.MediaCollectionManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.drop
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.size
import kotlinx.dnq.query.take
import java.util.SplittableRandom

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class RandomMediaItemHandler : AbstractCollectionHandler(), GetRestHandler<ApiMediaItem> {

    override val route: String = "collection/{collectionId}/random"

    @OpenApi(
        summary = "Selects and returns a random media item from a given media collection.",
        path = "/api/v2/collection/{collectionId}/random",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam("collectionId", String::class, "Collection ID", required = true)
        ],
        tags = ["Collection"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiMediaItem::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): ApiMediaItem =
        MediaCollectionManager.getRandomMediaItem(collectionId(ctx)) ?: throw ErrorStatusException(
            404,
            "Failed to fetch media item. It seems that the given collection is empty.",
            ctx
        )

}

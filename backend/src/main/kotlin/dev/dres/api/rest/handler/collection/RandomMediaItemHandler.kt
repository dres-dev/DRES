package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
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
class RandomMediaItemHandler(store: TransientEntityStore) : AbstractCollectionHandler(store), GetRestHandler<ApiMediaItem> {

    private val rand = SplittableRandom(System.currentTimeMillis())

    override val route: String = "collection/{collectionId}/random"

    @OpenApi(
        summary = "Selects and returns a random media item from a given media collection.",
        path = "/api/v1/collection/{collectionId}/random",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam("collectionId", String::class, "Collection ID")
        ],
        tags = ["Collection"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiMediaItem::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): ApiMediaItem = this.store.transactional(true) {
        val collection = collectionFromContext(ctx)
        val item = collection.items.drop(this.rand.nextInt(0, collection.items.size())).take(1).firstOrNull() ?:
            throw ErrorStatusException(404, "Failed to ferch media item. It seems that the given collection ${collection.id} is empty.", ctx)
        item.toApi()
    }
}
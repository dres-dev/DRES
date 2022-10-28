package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.collection.RestFullMediaCollection
import dev.dres.api.rest.types.collection.RestMediaCollection
import dev.dres.api.rest.types.collection.RestMediaItem
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.basics.media.MediaItem
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.query

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ShowCollectionHandler(store: TransientEntityStore) : AbstractCollectionHandler(store), GetRestHandler<RestFullMediaCollection> {

    override val route: String = "collection/{collectionId}"

    @OpenApi(
        summary = "Shows the content of the specified media collection.",
        path = "/api/v1/collection/{collectionId}",
        pathParams = [OpenApiParam("collectionId", String::class, "Collection ID")],
        tags = ["Collection"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(RestFullMediaCollection::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): RestFullMediaCollection = this.store.transactional(true) {
        val collection = collectionFromContext(ctx) //also checks if collection exists
        val items = MediaItem.query(MediaItem::collection eq collection).asSequence().map { RestMediaItem.fromMediaItem(it) }.toList()
        RestFullMediaCollection(RestMediaCollection.fromMediaCollection(collection), items)
    }
}
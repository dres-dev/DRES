package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.collection.ApiPopulatedMediaCollection
import dev.dres.api.rest.types.collection.ApiMediaCollection
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.media.DbMediaItem
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
class ShowCollectionHandler(store: TransientEntityStore) : AbstractCollectionHandler(store), GetRestHandler<ApiPopulatedMediaCollection> {

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
    override fun doGet(ctx: Context): ApiPopulatedMediaCollection = this.store.transactional(true) {
        val collection = collectionFromContext(ctx) //also checks if collection exists
        val items = DbMediaItem.query(DbMediaItem::collection eq collection).asSequence().map { it.toApi() }.toList()
        ApiPopulatedMediaCollection(ApiMediaCollection.fromMediaCollection(collection), items)
    }
}

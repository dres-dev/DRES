package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.media.DbMediaItem
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.query
import kotlinx.dnq.query.startsWith
import kotlinx.dnq.query.take

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ListMediaItemHandler(store: TransientEntityStore) : AbstractCollectionHandler(store), GetRestHandler<List<ApiMediaItem>> {
    override val route: String = "collection/{collectionId}/{startsWith}"

    @OpenApi(
        summary = "Lists media items from a given media collection whose name start with the given string.",
        path = "/api/v2/collection/{collectionId}/{startsWith}",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam("collectionId", String::class, "Collection ID"),
            OpenApiParam("startsWith", String::class, "Name the item(s) should start with.", required = false)
        ],
        tags = ["Collection"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiMediaItem>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): List<ApiMediaItem> = this.store.transactional(true) {
        val collection = collectionFromContext(ctx)
        val start = ctx.pathParamMap()["startsWith"]
        val query = if (!start.isNullOrBlank()) {
            collection.items.query(DbMediaItem::name startsWith start)
        } else {
            collection.items
        }
        query.take(50).asSequence().map { it.toApi() }.toList()
    }
}
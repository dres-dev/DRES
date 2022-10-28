package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.collection.RestMediaCollection
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.media.MediaCollection
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ListCollectionHandler(store: TransientEntityStore) : AbstractCollectionHandler(store), GetRestHandler<List<RestMediaCollection>> {

    override val route: String = "collection/list"

    @OpenApi(
        summary = "Lists all available media collections with basic information about their content.",
        path = "/api/v1/collection/list",
        tags = ["Collection"],
        methods = [HttpMethod.GET],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<RestMediaCollection>::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): List<RestMediaCollection> {
        return this.store.transactional(true) {
            MediaCollection.all().asSequence().map { RestMediaCollection.fromMediaCollection(it) }.toList()
        }
    }
}
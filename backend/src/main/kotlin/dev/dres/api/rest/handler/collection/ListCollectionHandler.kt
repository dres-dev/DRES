package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.collection.ApiMediaCollection
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.media.DbMediaCollection
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
class ListCollectionHandler(store: TransientEntityStore) : AbstractCollectionHandler(store), GetRestHandler<List<ApiMediaCollection>> {

    override val route: String = "collection/list"

    @OpenApi(
        summary = "Lists all available media collections with basic information about their content.",
        path = "/api/v2/collection/list",
        tags = ["Collection"],
        methods = [HttpMethod.GET],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiMediaCollection>::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): List<ApiMediaCollection> {
        return this.store.transactional(true) {
            DbMediaCollection.all().asSequence().map { ApiMediaCollection.fromMediaCollection(it) }.toList()
        }
    }
}

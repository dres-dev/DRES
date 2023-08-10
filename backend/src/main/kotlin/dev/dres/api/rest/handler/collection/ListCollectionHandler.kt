package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.collection.ApiMediaCollection
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.media.DbMediaCollection
import dev.dres.mgmt.MediaCollectionManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ListCollectionHandler : AbstractCollectionHandler(), GetRestHandler<List<ApiMediaCollection>> {

    override val route: String = "collection/list"

    @OpenApi(
        summary = "Lists all available media collections with basic information about their content.",
        path = "/api/v2/collection/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Collection"],
        methods = [HttpMethod.GET],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiMediaCollection>::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): List<ApiMediaCollection> = MediaCollectionManager.getCollections()
}

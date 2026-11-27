package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.collection.ApiMediaCollection
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.media.DbMediaCollection
import dev.dres.mgmt.MediaCollectionManager
import dev.dres.utilities.extensions.cleanPathString
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class AddCollectionHandler() : AbstractCollectionHandler(), PostRestHandler<SuccessStatus> {

    override val route: String = "collection"

    @OpenApi(
        summary = "Adds a new media collection.",
        path = "/api/v2/collection",
        tags = ["Collection"],
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.POST],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiMediaCollection::class)]),
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val restCollection = try {
            ctx.bodyAsClass(ApiMediaCollection::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        if (restCollection.basePath == null) {
            throw ErrorStatusException(400, "Invalid parameters, collection base path not set.", ctx)
        }

        val collection = try {
            MediaCollectionManager.createCollection(
                restCollection.name, restCollection.description, restCollection.basePath.cleanPathString()
            )
        } catch (e: IllegalArgumentException) {
            /* A duplicate name or path is a conflict with existing state, not a malformed request. */
            throw ErrorStatusException(400, e.message ?: "Could not create collection.", ctx)
        }

        return SuccessStatus("Collection ${collection.id} added successfully.")
    }
}

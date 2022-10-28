package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.collection.RestMediaCollection
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.UID
import dev.dres.data.model.media.MediaCollection
import dev.dres.data.model.media.MediaItem
import dev.dres.utilities.extensions.cleanPathString
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import java.util.UUID

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class AddCollectionHandler(store: TransientEntityStore) : AbstractCollectionHandler(store), PostRestHandler<SuccessStatus> {

    override val route: String = "collection"

    @OpenApi(
        summary = "Adds a new media collection.",
        path = "/api/v1/collection",
        tags = ["Collection"],
        methods = [HttpMethod.POST],
        requestBody = OpenApiRequestBody([OpenApiContent(RestMediaCollection::class)]),
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val restCollection = try {
            ctx.bodyAsClass(RestMediaCollection::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        if (restCollection.basePath == null) {
            throw ErrorStatusException(400, "Invalid parameters, collection base path not set.", ctx)
        }

        val collection = this.store.transactional {
            MediaCollection.new {
                this.id = UUID.randomUUID().toString()
                this.name = restCollection.name
                this.description = restCollection.description
                this.path = restCollection.basePath.cleanPathString()
            }
        }
        return SuccessStatus("Collection ${collection.id} added successfully.")
    }
}
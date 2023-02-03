package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.PatchRestHandler
import dev.dres.api.rest.types.collection.ApiMediaCollection
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.media.DbMediaCollection
import dev.dres.utilities.extensions.cleanPathString
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class UpdateCollectionHandler(store: TransientEntityStore) : AbstractCollectionHandler(store), PatchRestHandler<SuccessStatus> {

    override val route: String = "collection"

    @OpenApi(
        summary = "Updates a media collection",
        path = "/api/v2/collection",
        tags = ["Collection"],
        methods = [HttpMethod.PATCH],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiMediaCollection::class)]),
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPatch(ctx: Context): SuccessStatus {

        val restCollection = try {
            ctx.bodyAsClass(ApiMediaCollection::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        val status = this.store.transactional {
            val collection = DbMediaCollection.query(DbMediaCollection::id eq restCollection.id).firstOrNull()
                ?: throw ErrorStatusException(400, "Invalid parameters, collection with ID ${restCollection.id} does not exist.", ctx)
            collection.name = restCollection.name.trim()
            collection.description = restCollection.description?.trim() ?: collection.description
            collection.path = restCollection.basePath?.cleanPathString() ?: collection.path
            SuccessStatus("Collection ${collection.id} updated successfully.")
        }

        return status
    }
}

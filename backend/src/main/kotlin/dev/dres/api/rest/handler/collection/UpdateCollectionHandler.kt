package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.PatchRestHandler
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
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class UpdateCollectionHandler : AbstractCollectionHandler(),
    PatchRestHandler<SuccessStatus> {

    override val route: String = "collection"

    @OpenApi(
        summary = "Updates a media collection",
        path = "/api/v2/collection",
        operationId = OpenApiOperation.AUTO_GENERATE,
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

        try {
            MediaCollectionManager.updateCollection(restCollection)
            return SuccessStatus("Collection ${restCollection.id} updated successfully.")
        } catch (e: Exception) {
            throw ErrorStatusException(400, e.message ?: "Could not update collection", ctx)
        }

    }
}

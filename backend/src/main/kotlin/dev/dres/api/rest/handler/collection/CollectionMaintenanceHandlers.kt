package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.DeleteRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.collection.*
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.mgmt.MediaCollectionMaintenance
import dev.dres.mgmt.MediaCollectionManager
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*

/** Starts scanning the folder of a collection for new media files. The scan runs in the background. */
class StartCollectionScanHandler : AbstractCollectionHandler(), PostRestHandler<ApiCollectionScanStatus> {
    override val route: String = "collection/{collectionId}/scan"

    @OpenApi(
        summary = "Starts scanning the folder of a media collection and adds all new files with the given endings.",
        path = "/api/v2/collection/{collectionId}/scan",
        pathParams = [OpenApiParam("collectionId", String::class, "Collection ID", required = true)],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiCollectionScanRequest::class)]),
        tags = ["Collection"],
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.POST],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiCollectionScanStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("409", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): ApiCollectionScanStatus {
        val collectionId = collectionId(ctx)
        if (MediaCollectionManager.getCollection(collectionId) == null) {
            throw ErrorStatusException(404, "Collection not found.", ctx)
        }
        val request = try {
            ctx.bodyAsClass(ApiCollectionScanRequest::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid scan request.", ctx)
        }
        return try {
            MediaCollectionMaintenance.startScan(collectionId, request.videoTypes, request.imageTypes)
        } catch (e: IllegalArgumentException) {
            throw ErrorStatusException(400, e.message ?: "Invalid scan request.", ctx)
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(409, e.message ?: "A scan is already running.", ctx)
        }
    }
}

/** Requests cancellation of a running scan. The background task stops between files/chunks. */
class CancelCollectionScanHandler : AbstractCollectionHandler(), PostRestHandler<ApiCollectionScanStatus> {
    override val route: String = "collection/{collectionId}/scan/cancel"

    @OpenApi(
        summary = "Cancels a running scan of a media collection.",
        path = "/api/v2/collection/{collectionId}/scan/cancel",
        pathParams = [OpenApiParam("collectionId", String::class, "Collection ID", required = true)],
        tags = ["Collection"],
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.POST],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiCollectionScanStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): ApiCollectionScanStatus {
        val id = collectionId(ctx)
        if (MediaCollectionManager.getCollection(id) == null) {
            throw ErrorStatusException(404, "Collection not found.", ctx)
        }
        return MediaCollectionMaintenance.cancelScan(id)
    }
}

/** Returns progress and result of the most recent scan of a collection. */
class CollectionScanStatusHandler : AbstractCollectionHandler(), GetRestHandler<ApiCollectionScanStatus> {
    override val route: String = "collection/{collectionId}/scan/status"

    @OpenApi(
        summary = "Returns progress and result of the most recent scan of a media collection.",
        path = "/api/v2/collection/{collectionId}/scan/status",
        pathParams = [OpenApiParam("collectionId", String::class, "Collection ID", required = true)],
        tags = ["Collection"],
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.GET],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiCollectionScanStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): ApiCollectionScanStatus = MediaCollectionMaintenance.scanStatus(collectionId(ctx))
}

/** Checks that all files of a collection exist and are readable. */
class CheckCollectionHandler : AbstractCollectionHandler(), PostRestHandler<ApiCollectionCheckResult> {
    override val route: String = "collection/{collectionId}/check"

    @OpenApi(
        summary = "Checks that the files of all items of a media collection exist and are readable.",
        path = "/api/v2/collection/{collectionId}/check",
        pathParams = [OpenApiParam("collectionId", String::class, "Collection ID", required = true)],
        tags = ["Collection"],
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.POST],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiCollectionCheckResult::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): ApiCollectionCheckResult =
        MediaCollectionMaintenance.check(collectionId(ctx)) ?: throw ErrorStatusException(404, "Collection not found.", ctx)
}

/** Deletes all items of a collection that are not used by templates or submissions. */
class DeleteCollectionItemsHandler : AbstractCollectionHandler(), DeleteRestHandler<ApiCollectionItemsDeletion> {
    override val route: String = "collection/{collectionId}/items"

    @OpenApi(
        summary = "Deletes all items of a media collection. Items still used by templates or submissions are kept and reported.",
        path = "/api/v2/collection/{collectionId}/items",
        pathParams = [OpenApiParam("collectionId", String::class, "Collection ID", required = true)],
        tags = ["Collection"],
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.DELETE],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiCollectionItemsDeletion::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doDelete(ctx: Context): ApiCollectionItemsDeletion =
        MediaCollectionMaintenance.deleteItems(collectionId(ctx)) ?: throw ErrorStatusException(404, "Collection not found.", ctx)
}

/** Provides the items of a collection as CSV file (same format as the CLI export). */
class CollectionExportHandler : AbstractCollectionHandler(), GetRestHandler<Unit> {
    override val route: String = "download/collection/{collectionId}"

    override fun doGet(ctx: Context) {
        //nop
    }

    @OpenApi(
        summary = "Provides a CSV file with all items of a media collection (type, name, location, duration, fps).",
        path = "/api/v2/download/collection/{collectionId}",
        pathParams = [OpenApiParam("collectionId", String::class, "Collection ID", required = true)],
        tags = ["Download"],
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.GET],
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class, type = "text/csv")]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun get(ctx: Context) {
        val collectionId = collectionId(ctx)
        val csv = MediaCollectionMaintenance.exportCsv(collectionId)
        if (csv == null) {
            ctx.status(404)
            ctx.json(ErrorStatus("Collection $collectionId not found."))
            return
        }
        ctx.contentType("text/csv")
        ctx.header("Content-Disposition", "attachment; filename=\"collection-$collectionId.csv\"")
        ctx.result(csv)
    }
}

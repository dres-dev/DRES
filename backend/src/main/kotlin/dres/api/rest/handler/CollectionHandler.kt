package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.data.dbo.DAO
import dres.data.model.basics.MediaCollection
import dres.data.model.basics.MediaItem
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse


abstract class CollectionHandler(protected val collections: DAO<MediaCollection>, protected val items: DAO<MediaItem>) : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.ADMIN)

    private fun collectionId(ctx: Context): Long =
            ctx.pathParamMap().getOrElse("collectionId") {
                throw ErrorStatusException(404, "Parameter 'collectionId' is missing!'")
            }.toLong()

    private fun collectionById(id: Long): MediaCollection =
            collections[id] ?: throw ErrorStatusException(404, "Collection with ID $id not found.'")

    protected fun collectionFromContext(ctx: Context): MediaCollection = collectionById(collectionId(ctx))

}

class ListCollectionHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>) : CollectionHandler(collections, items), GetRestHandler<List<MediaCollection>> {

    @OpenApi(
            summary = "Lists all available Media Collections with basic information about their content.",
            path = "/api/collection",
            tags = ["Collection"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<MediaCollection>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context)  = collections.toList()

    override val route: String = "collection"
}

class ShowCollectionHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>) : CollectionHandler(collections, items), GetRestHandler<List<MediaItem>> {

    @OpenApi(
            summary = "Lists all available Media Collections with basic information about their content.",
            path = "/api/collection/:collectionId",
            pathParams = [OpenApiParam("collectionId", Long::class, "Collection ID")],
            tags = ["Collection"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<MediaItem>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): List<MediaItem> {

        val collection = collectionFromContext(ctx) //also checks if collection exists

        return items.filter { it.collection == collection.id }.toList()

    }

    override val route: String = "collection/:collectionId"
}
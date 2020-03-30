package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.types.status.SuccessStatus
import dres.data.dbo.DAO
import dres.data.model.basics.MediaCollection
import dres.data.model.basics.MediaItem
import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*


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
    override fun doGet(ctx: Context)  = this.collections.toList()

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

class AddMediaItemHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>) : CollectionHandler(collections, items), PostRestHandler<SuccessStatus> {

    @OpenApi(
            summary = "Adds a Media Item to the specified Media Collection.",
            path = "/api/collection/:collectionId", method = HttpMethod.POST,
            pathParams = [OpenApiParam("collectionId", Long::class, "Collection ID")],
            requestBody = OpenApiRequestBody([OpenApiContent(MediaItem::class)]),
            tags = ["Collection"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<MediaItem>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {

        val collection = collectionFromContext(ctx)

        val mediaItem = try {
            ctx.bodyAsClass(MediaItem::class.java)
        }catch (e: BadRequestResponse){
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!")
        }

        val existing = items.find { it.collection == collection.id && it.name == mediaItem.name }
        if (existing != null) {
            throw ErrorStatusException(400, "item with name '${mediaItem.name}' already exists in collection: $existing")
        }

        val toAdd: MediaItem = if (mediaItem.collection == collection.id) {
            mediaItem
        } else {
            mediaItem.withCollection(collection.id)
        }

        items.append(toAdd)
        return SuccessStatus("Media Item added")

    }

    override val route: String = "collection/:collectionId"
}

class ListMediaItemHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>) : CollectionHandler(collections, items), GetRestHandler<Array<MediaItem>> {
    @OpenApi(
            summary = "Adds a Media Item to the specified Media Collection.",
            path = "/api/collection/:collectionId/:startsWith", method = HttpMethod.GET,
            pathParams = [
                OpenApiParam("collectionId", Long::class, "Collection ID"),
                OpenApiParam("startsWith", String::class, "Name starts with")
            ],
            requestBody = OpenApiRequestBody([OpenApiContent(MediaItem::class)]),
            tags = ["Collection"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<MediaItem>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): Array<MediaItem> {
        val collection = collectionFromContext(ctx)
        val startsWith = ctx.pathParamMap()["startsWith"]

        val results =  if (startsWith!= null) {
            this.items.filter {
                it.collection == collection.id && it.name.startsWith(startsWith)
            }.take(50).toTypedArray()
        } else {
            this.items.filter {
                it.collection == collection.id
            }.take(50).toTypedArray()
        }
        return results
    }
    override val route: String = "collection/:collectionId/:startsWith"
}
package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.types.collection.RestMediaCollection
import dres.api.rest.types.collection.RestMediaItem
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.types.status.SuccessStatus
import dres.data.dbo.DAO
import dres.data.dbo.DaoIndexer
import dres.data.model.UID
import dres.data.model.basics.media.MediaCollection
import dres.data.model.basics.media.MediaItem
import dres.utilities.extensions.UID
import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*
import kotlin.random.Random

abstract class CollectionHandler(protected val collections: DAO<MediaCollection>, protected val items: DAO<MediaItem>) : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.ADMIN)

    private fun collectionId(ctx: Context): UID =
            ctx.pathParamMap().getOrElse("collectionId") {
                throw ErrorStatusException(404, "Parameter 'collectionId' is missing!'")
            }.UID()

    private fun collectionById(id: UID): MediaCollection =
            collections[id] ?: throw ErrorStatusException(404, "Collection with ID $id not found.'")

    protected fun collectionFromContext(ctx: Context): MediaCollection = collectionById(collectionId(ctx))

}

class ListCollectionHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>) : CollectionHandler(collections, items), GetRestHandler<List<RestMediaCollection>> {

    @OpenApi(
            summary = "Lists all available media collections with basic information about their content.",
            path = "/api/collection",
            tags = ["Collection"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<RestMediaCollection>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context) = this.collections.map { RestMediaCollection.fromMediaCollection(it) }

    override val route: String = "collection"
}

class ShowCollectionHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>) : CollectionHandler(collections, items), GetRestHandler<List<RestMediaItem>> {

    @OpenApi(
            summary = "Shows the content of the specified media collection.",
            path = "/api/collection/:collectionId",
            pathParams = [OpenApiParam("collectionId", UID::class, "Collection ID")],
            tags = ["Collection"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<RestMediaItem>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): List<RestMediaItem> {
        val collection = collectionFromContext(ctx) //also checks if collection exists
        return items.filter { it.collection == collection.id }.map { RestMediaItem.fromMediaItem(it) }
    }

    override val route: String = "collection/:collectionId"
}

class AddMediaItemHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>) : CollectionHandler(collections, items), PostRestHandler<SuccessStatus> {

    @OpenApi(
            summary = "Adds a Media Item to the specified Media Collection.",
            path = "/api/collection/:collectionId", method = HttpMethod.POST,
            pathParams = [OpenApiParam("collectionId", UID::class, "Collection ID")],
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
        } catch (e: BadRequestResponse) {
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

class ListMediaItemHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>, private val collectionItems: DaoIndexer<MediaItem, UID>) : CollectionHandler(collections, items), GetRestHandler<List<RestMediaItem>> {
    @OpenApi(
            summary = "Lists media items from a given media collection whose name start with the given string.",
            path = "/api/collection/:collectionId/:startsWith", method = HttpMethod.GET,
            pathParams = [
                OpenApiParam("collectionId", UID::class, "Collection ID"),
                OpenApiParam("startsWith", String::class, "Name starts with", required = false)
            ],
            tags = ["Collection"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<RestMediaItem>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): List<RestMediaItem> {
        val collection = collectionFromContext(ctx)
        val startsWith = ctx.pathParamMap()["startsWith"]
        val items = this.collectionItems[collection.id]

        return if (!startsWith.isNullOrBlank()) {
            items.filter {
                it.collection == collection.id && it.name.startsWith(startsWith)
            }.take(50).map { RestMediaItem.fromMediaItem(it) }
        } else {
            items.filter {
                it.collection == collection.id
            }.take(50).map { RestMediaItem.fromMediaItem(it) }
        }
    }

    override val route: String = "collection/:collectionId/:startsWith"
}

class GetMediaItemHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>) : CollectionHandler(collections, items), GetRestHandler<RestMediaItem> {

    @OpenApi(
            summary = "Selects and returns a specific media item from a given media collection.",
            path = "/api/collection/:collectionId/media/:mediaId", method = HttpMethod.GET,
            pathParams = [
                OpenApiParam("collectionId", UID::class, "Collection ID"),
                OpenApiParam("mediaId", UID::class, "Media item ID")
            ],
            tags = ["Collection"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(RestMediaItem::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): RestMediaItem {
        val collection = collectionFromContext(ctx)
        val mediaId = ctx.pathParamMap().getOrElse("mediaId") {
            throw ErrorStatusException(404, "Parameter 'mediaId' is missing!'")
        }.UID()
        val item = this.items[mediaId] ?:  throw ErrorStatusException(404, "Media item with ID $mediaId not found.'")
        if (item.collection != collection.id) {
            throw ErrorStatusException(400, "Collection ID of media item and collection don't match!'")
        }
        return RestMediaItem.fromMediaItem(item)
    }

    override val route: String = "collection/:collectionId/media/:mediaId"
}

class RandomMediaItemHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>, private val collectionItems: DaoIndexer<MediaItem, UID>) : CollectionHandler(collections, items), GetRestHandler<RestMediaItem> {

    private val rand = Random(System.currentTimeMillis()) // TODO Decide upon seed -- time based or fixed?

    @OpenApi(
            summary = "Selects and returns a random media item from a given media collection.",
            path = "/api/collection/:collectionId/random", method = HttpMethod.GET,
            pathParams = [
                OpenApiParam("collectionId", UID::class, "Collection ID")
            ],
            tags = ["Collection"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(RestMediaItem::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): RestMediaItem {
        val collection = collectionFromContext(ctx)
        val items = this.collectionItems[collection.id]
        return RestMediaItem.fromMediaItem(items[rand.nextInt(items.size)])
    }

    override val route: String = "collection/:collectionId/random"
}
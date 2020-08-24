package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.types.collection.RestFullMediaCollection
import dres.api.rest.types.collection.RestMediaCollection
import dres.api.rest.types.collection.RestMediaItem
import dres.api.rest.types.collection.RestMediaItemType
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.types.status.SuccessStatus
import dres.data.dbo.DAO
import dres.data.dbo.DaoIndexer
import dres.data.model.Config
import dres.data.model.UID
import dres.data.model.basics.media.MediaCollection
import dres.data.model.basics.media.MediaItem
import dres.utilities.extensions.UID
import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.random.Random
import kotlin.streams.toList

abstract class CollectionHandler(protected val collections: DAO<MediaCollection>, protected val items: DAO<MediaItem>) : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.ADMIN)

    private fun collectionId(ctx: Context): UID =
            ctx.pathParamMap().getOrElse("collectionId") {
                throw ErrorStatusException(404, "Parameter 'collectionId' is missing!'", ctx)
            }.UID()

    private fun collectionById(id: UID, ctx: Context): MediaCollection =
            collections[id] ?: throw ErrorStatusException(404, "Collection with ID $id not found.'", ctx)

    protected fun collectionFromContext(ctx: Context): MediaCollection = collectionById(collectionId(ctx), ctx)

}

class ListCollectionHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>) : CollectionHandler(collections, items), GetRestHandler<List<RestMediaCollection>> {

    @OpenApi(
            summary = "Lists all available media collections with basic information about their content.",
            path = "/api/collection/list",
            tags = ["Collection"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<RestMediaCollection>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context) = this.collections.map { RestMediaCollection.fromMediaCollection(it) }

    override val route: String = "collection/list"
}

class AddCollectionHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>) : CollectionHandler(collections, items), PostRestHandler<SuccessStatus> {

    @OpenApi(
            summary = "Adds a new media collection",
            path = "/api/collection",
            tags = ["Collection"],
            method = HttpMethod.POST,
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

        if (collections.find { it.name == restCollection.name } != null) {
            throw ErrorStatusException(400, "Invalid parameters, collection with name ${restCollection.name} already exists.", ctx)
        }

        val collection = MediaCollection(UID.EMPTY, restCollection.name, restCollection.description, restCollection.basePath)
        collections.append(collection)

        return SuccessStatus("Collection added")

    }

    override val route: String = "collection"

}

class DeleteCollectionHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>) : CollectionHandler(collections, items), DeleteRestHandler<SuccessStatus> {

    @OpenApi(
            summary = "Deletes a media collection",
            path = "/api/collection/:collectionId",
            tags = ["Collection"],
            method = HttpMethod.DELETE,
            requestBody = OpenApiRequestBody([OpenApiContent(RestMediaCollection::class)]),
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doDelete(ctx: Context): SuccessStatus {
        val collection = collectionFromContext(ctx)

        collections.delete(collection)

        return SuccessStatus("Collection ${collection.id.string} deleted")
    }

    override val route: String = "collection/:collectionId"

}

class UpdateCollectionHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>) : CollectionHandler(collections, items), PatchRestHandler<SuccessStatus> {

    @OpenApi(
            summary = "Updates a media collection",
            path = "/api/collection",
            tags = ["Collection"],
            method = HttpMethod.PATCH,
            requestBody = OpenApiRequestBody([OpenApiContent(RestMediaCollection::class)]),
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPatch(ctx: Context): SuccessStatus {

        val restCollection = try {
            ctx.bodyAsClass(RestMediaCollection::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        val collection = collections[restCollection.id.UID()]
                ?: throw ErrorStatusException(400, "Invalid parameters, collection with ID ${restCollection.id} does not exist.", ctx)

        val updatedCollection = MediaCollection(collection.id, restCollection.name, restCollection.description ?: collection.description, restCollection.basePath ?: collection.basePath)
        collections.update(updatedCollection)

        return SuccessStatus("Collection updated")

    }

    override val route: String = "collection"

}

class ShowCollectionHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>) : CollectionHandler(collections, items), GetRestHandler<RestFullMediaCollection> {

    @OpenApi(
            summary = "Shows the content of the specified media collection.",
            path = "/api/collection/:collectionId",
            pathParams = [OpenApiParam("collectionId", UID::class, "Collection ID")],
            tags = ["Collection"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(RestFullMediaCollection::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): RestFullMediaCollection {
        val collection = collectionFromContext(ctx) //also checks if collection exists
        val items = items.filter { it.collection == collection.id }.map { RestMediaItem.fromMediaItem(it) }
        return RestFullMediaCollection(RestMediaCollection.fromMediaCollection(collection), items)
    }

    override val route: String = "collection/:collectionId"
}

class AddMediaItemHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>) : CollectionHandler(collections, items), PostRestHandler<SuccessStatus> {

    @OpenApi(
            summary = "Adds a Media Item to the specified Media Collection.",
            path = "/api/mediaItem", method = HttpMethod.POST,
            requestBody = OpenApiRequestBody([OpenApiContent(RestMediaItem::class)]),
            tags = ["Collection"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {

        val mediaItem = try {
            ctx.bodyAsClass(RestMediaItem::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        val collectionId = mediaItem.collectionId.UID()
        val existing = items.find { it.collection == collectionId && it.name == mediaItem.name }
        if (existing != null) {
            throw ErrorStatusException(400, "item with name '${mediaItem.name}' already exists in collection: $existing", ctx)
        }

        if (mediaItem.type == RestMediaItemType.VIDEO) {
            if (mediaItem.durationMs == null){
                throw ErrorStatusException(400, "Duration needs to be set for a video item", ctx)
            }
            if (mediaItem.fps == null){
                throw ErrorStatusException(400, "Frame rate needs to be set for a video item", ctx)
            }
        }

        items.append(mediaItem.toMediaItem())
        return SuccessStatus("Media Item added")

    }

    override val route: String = "mediaItem"
}

class UpdateMediaItemHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>) : CollectionHandler(collections, items), PatchRestHandler<SuccessStatus> {

    @OpenApi(
            summary = "Updates a Media Item to the specified Media Collection.",
            path = "/api/mediaItem", method = HttpMethod.PATCH,
            requestBody = OpenApiRequestBody([OpenApiContent(RestMediaItem::class)]),
            tags = ["Collection"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPatch(ctx: Context): SuccessStatus {

        val mediaItem = try {
            ctx.bodyAsClass(RestMediaItem::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        items[mediaItem.id.UID()] ?: throw ErrorStatusException(400, "item with name '${mediaItem.name}' does not exists", ctx)


        if (mediaItem.type == RestMediaItemType.VIDEO) {
            if (mediaItem.durationMs == null){
                throw ErrorStatusException(400, "Duration needs to be set for a video item", ctx)
            }
            if (mediaItem.fps == null){
                throw ErrorStatusException(400, "Frame rate needs to be set for a video item", ctx)
            }
        }

        items.update(mediaItem.toMediaItem())
        return SuccessStatus("Media Item updated")

    }

    override val route: String = "mediaItem"
}

class GetMediaItemHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>) : CollectionHandler(collections, items), GetRestHandler<RestMediaItem> {

    @OpenApi(
            summary = "Selects and returns a specific media item.",
            path = "/api/mediaItem/:mediaId", method = HttpMethod.GET,
            pathParams = [
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
        val mediaId = ctx.pathParamMap().getOrElse("mediaId") {
            throw ErrorStatusException(404, "Parameter 'mediaId' is missing!'", ctx)
        }.UID()
        val item = this.items[mediaId] ?:  throw ErrorStatusException(404, "Media item with ID $mediaId not found.'", ctx)

        return RestMediaItem.fromMediaItem(item)
    }

    override val route: String = "mediaItem/:mediaId"
}

class DeleteMediaItemHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>) : CollectionHandler(collections, items), DeleteRestHandler<SuccessStatus> {

    @OpenApi(
            summary = "Selects and returns a specific media item.",
            path = "/api/mediaItem/:mediaId", method = HttpMethod.DELETE,
            pathParams = [
                OpenApiParam("mediaId", UID::class, "Media item ID")
            ],
            tags = ["Collection"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doDelete(ctx: Context): SuccessStatus {
        val mediaId = ctx.pathParamMap().getOrElse("mediaId") {
            throw ErrorStatusException(404, "Parameter 'mediaId' is missing!'", ctx)
        }.UID()
        val item = this.items[mediaId] ?:  throw ErrorStatusException(404, "Media item with ID $mediaId not found.'", ctx)

        this.items.delete(item)

        return SuccessStatus("Item deleted")
    }

    override val route: String = "mediaItem/:mediaId"
}

class ListMediaItemHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>, private val mediaItemCollectionNameIndex: DaoIndexer<MediaItem, Pair<UID, String>>) : CollectionHandler(collections, items), GetRestHandler<List<RestMediaItem>> {
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
        val names = mediaItemCollectionNameIndex.keys().filter { it.first == collection.id }

        return if (!startsWith.isNullOrBlank()) {
            names.filter {
                it.second.startsWith(startsWith)
            }.take(50).map { RestMediaItem.fromMediaItem(mediaItemCollectionNameIndex[it].first()) }
        } else {
            names.take(50).map { RestMediaItem.fromMediaItem(mediaItemCollectionNameIndex[it].first()) }
        }
    }

    override val route: String = "collection/:collectionId/:startsWith"
}

class RandomMediaItemHandler(collections: DAO<MediaCollection>, items: DAO<MediaItem>, private val mediaItemCollectionUidIndex: DaoIndexer<MediaItem, Pair<UID, UID>>) : CollectionHandler(collections, items), GetRestHandler<RestMediaItem> {

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
        val ids = mediaItemCollectionUidIndex.keys().filter { it.first == collection.id }
        val itemId = ids[rand.nextInt(ids.size)].second
        return RestMediaItem.fromMediaItem(items[itemId]!!)
    }

    override val route: String = "collection/:collectionId/random"
}

/**
 * Lists and returns the media items in the external media item directory.
 */
class ListExternalItemHandler(config: Config) : GetRestHandler<Array<String>> {

    /** Path to the directory that contains the external items. */
    val path = Paths.get(config.externalPath)

    init {
        /* Check if directory exists and create it, if it doesn't. */
        if (!Files.exists(this.path)) {
            Files.createDirectories(this.path)
        }
    }

    @OpenApi(
            summary = "Lists items from the external media collection whose name start with the given string.",
            path = "/api/external/:startsWith", method = HttpMethod.GET,
            pathParams = [
                OpenApiParam("startsWith", String::class, "Name starts with.", required = false)
            ],
            tags = ["Collection"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<String>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): Array<String> {
        val startsWith = ctx.pathParamMap()["startsWith"] ?: ""
        val list = Files.walk(this.path, 1, FileVisitOption.FOLLOW_LINKS).filter {
            Files.isRegularFile(it) && it.fileName.startsWith(startsWith)
        }.limit(50).map { it.toString() }.toList()
        return list.toTypedArray()
    }

    override val route: String = "external/:startsWith"
}
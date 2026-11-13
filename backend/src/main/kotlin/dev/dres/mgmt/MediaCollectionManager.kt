package dev.dres.mgmt

import dev.dres.api.rest.types.collection.ApiMediaCollection
import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.api.rest.types.collection.ApiMediaSegment
import dev.dres.api.rest.types.collection.ApiPopulatedMediaCollection
import dev.dres.data.model.media.*
import dev.dres.utilities.extensions.cleanPathString
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import java.lang.Exception
import kotlin.random.Random

object MediaCollectionManager {

    private lateinit var store: TransientEntityStore

    fun init(store: TransientEntityStore) {
        this.store = store
    }

    fun getCollections(): List<ApiMediaCollection> = this.store.transactional(true) {
        DbMediaCollection.all().asSequence().map { ApiMediaCollection.fromMediaCollection(it) }.toList()
    }

    fun getCollection(collectionId: CollectionId): ApiMediaCollection? = this.store.transactional(true) {
        DbMediaCollection.query(DbMediaCollection::id eq collectionId).firstOrNull()?.toApi()
    }

    fun getCollectionByName(name: String): ApiMediaCollection? = this.store.transactional(true){
        DbMediaCollection.query(DbMediaCollection::name eq name).firstOrNull()?.toApi()
    }

    fun getPopulatedCollection(collectionId: CollectionId): ApiPopulatedMediaCollection? =
        this.store.transactional(true) {
            val collection = DbMediaCollection.query(DbMediaCollection::id eq collectionId).firstOrNull()
                ?: return@transactional null
            val items =
                DbMediaItem.query(DbMediaItem::collection eq collection).asSequence().map { it.toApi() }.toList()
            ApiPopulatedMediaCollection(ApiMediaCollection.fromMediaCollection(collection), items)
        }

    fun createCollection(name: String, description: String?, basePath: String): ApiMediaCollection? =
        /* Unique constraints (name, path) are checked when the transaction is committed, hence the try has to enclose the transaction. */
            try {
            this.store.transactional {
                DbMediaCollection.new {
                    this.name = name
                    this.description = description
                    this.path = basePath.cleanPathString()
                }.toApi()
            }
            } catch (e: Exception) {
                null
            }

    fun updateCollection(apiMediaCollection: ApiMediaCollection) {
        this.store.transactional {
            val collection = DbMediaCollection.query(DbMediaCollection::id eq apiMediaCollection.id).firstOrNull()
                ?: throw IllegalArgumentException(
                    "Invalid parameters, collection with ID ${apiMediaCollection.id} does not exist.",
                )
            collection.name = apiMediaCollection.name.trim()
            collection.description = apiMediaCollection.description?.trim() ?: collection.description
            collection.path = apiMediaCollection.basePath?.cleanPathString() ?: collection.path
        }
    }

    fun deleteCollection(collectionId: CollectionId): ApiMediaCollection? = this.store.transactional {
        val collection =
            DbMediaCollection.query(DbMediaCollection::id eq collectionId).firstOrNull() ?: return@transactional null
        val api = collection.toApi()
        collection.delete()
        api
    }

    fun addMediaItem(mediaItem: ApiMediaItem) {

        val collectionId = mediaItem.collectionId

        this.store.transactional {
            val collection = DbMediaCollection.query(DbMediaCollection::id eq collectionId).firstOrNull()
                ?: throw IllegalArgumentException("Invalid parameters, collection with ID $collectionId does not exist.")
            if (collection.items.filter { it.name eq mediaItem.name }.isNotEmpty) {
                throw IllegalArgumentException("Media item with name '${mediaItem.name}' already exists in collection ${collection.name}.")
            }

            val item = DbMediaItem.new {
                this.type = mediaItem.type.toDb()
                this.name = mediaItem.name
                this.location = mediaItem.location
                this.fps = mediaItem.fps
                this.durationMs = mediaItem.durationMs
            }
            collection.items.add(item)
        }

    }

    /** Adds media items that do not conflict by either location or collection-unique name. Returns the count added. */
    fun addMediaItems(collectionId: CollectionId, mediaItems: Collection<ApiMediaItem>): Int = this.store.transactional {
        val collection = DbMediaCollection.query(DbMediaCollection::id eq collectionId).firstOrNull()
            ?: throw IllegalArgumentException("Invalid parameters, collection with ID $collectionId does not exist.")

        val knownItems = collection.items.asSequence().toList()
        val knownLocations = knownItems.mapTo(mutableSetOf()) { it.location }
        val knownNames = knownItems.mapTo(mutableSetOf()) { it.name }
        var added = 0

        mediaItems.forEach { mediaItem ->
            if (mediaItem.location in knownLocations || mediaItem.name in knownNames) return@forEach
            knownLocations.add(mediaItem.location)
            knownNames.add(mediaItem.name)
            val item = DbMediaItem.new {
                this.type = mediaItem.type.toDb()
                this.name = mediaItem.name
                this.location = mediaItem.location
                this.fps = mediaItem.fps
                this.durationMs = mediaItem.durationMs
            }
            collection.items.add(item)
            added++
        }
        added
    }

    fun getMediaItem(mediaItemId: MediaItemId): ApiMediaItem? = this.store.transactional(true) {
        DbMediaItem.query(DbMediaItem::id eq mediaItemId).firstOrNull()?.toApi()
    }

    fun getMediaItemsByName(collectionId: CollectionId, names: List<String>): List<ApiMediaItem> =
        this.store.transactional(true) {
            val collection = DbMediaCollection.query(DbMediaCollection::id eq collectionId).firstOrNull()
                ?: return@transactional emptyList<ApiMediaItem>()
            collection.items.filter { it.name isIn names }.asSequence().map { it.toApi() }.toList()
        }

    fun getMediaItemsByPartialName(collectionId: CollectionId, name: String?, limit: Int = 50): List<ApiMediaItem> =
        this.store.transactional(true) {
            val collection = DbMediaCollection.query(DbMediaCollection::id eq collectionId).firstOrNull()
                ?: return@transactional emptyList<ApiMediaItem>()
            if (!name.isNullOrBlank()) {
                collection.items.query(DbMediaItem::name startsWith name).sortedBy(DbMediaItem::name)
            } else {
                collection.items
            }.take(limit).asSequence().map { it.toApi() }.toList()
        }

    fun getRandomMediaItem(collectionId: CollectionId): ApiMediaItem? = this.store.transactional(true) {
        val collection =
            DbMediaCollection.query(DbMediaCollection::id eq collectionId).firstOrNull() ?: return@transactional null
        val size = collection.items.size()
        if (size == 0) return@transactional null
        collection.items.drop(Random.nextInt(0, size)).take(1).firstOrNull()?.toApi()
    }

    fun updateMediaItem(mediaItem: ApiMediaItem) {

        val collectionId = mediaItem.collectionId

        this.store.transactional {
            val collection = DbMediaCollection.query(DbMediaCollection::id eq collectionId).firstOrNull()
                ?: throw IllegalArgumentException("Invalid parameters, collection with ID $collectionId does not exist.")
            /* The item that is updated must not be considered a duplicate of itself. */
            if (collection.items.filter { (it.name eq mediaItem.name) and (it.id ne mediaItem.mediaItemId) }.isNotEmpty) {
                throw IllegalArgumentException("Media item with name '${mediaItem.name}' already exists in collection ${collection.name}.")
            }

            val item = DbMediaItem.query(DbMediaItem::id eq mediaItem.mediaItemId).firstOrNull()
                ?: throw IllegalArgumentException("Media item with ID ${mediaItem.mediaItemId} not found.")

            item.type = mediaItem.type.toDb()
            item.name = mediaItem.name
            item.location = mediaItem.location
            item.fps = mediaItem.fps
            item.durationMs = mediaItem.durationMs

        }

    }

    fun deleteMediaItem(mediaItemId: MediaItemId) = this.store.transactional {
        val item = DbMediaItem.query(DbMediaItem::id eq mediaItemId).firstOrNull()
            ?: throw IllegalArgumentException("Media item with ID $mediaItemId not found.")
        item.delete()
    }

    /**
     * Adds segments, returns the number of segments added
     */
    fun addSegments(collectionId: CollectionId, segments: Collection<ApiMediaSegment>): Int = this.store.transactional {
        var counter = 0
        val collection =
            DbMediaCollection.query(DbMediaCollection::id eq collectionId).firstOrNull() ?: return@transactional 0
        segments.groupBy { it.mediaItemName }.forEach { entry ->
            val videoItem = collection.items.filter { it.name eq entry.key }.firstOrNull()
            if (videoItem != null) {

                entry.value.filter { videoItem.segments.filter { db -> db.name eq it.segmentName }.isEmpty }
                    .forEach { segment ->
                        videoItem.segments.add(
                            DbMediaSegment.new {
                                this.name = segment.segmentName
                                this.start = segment.start
                                this.end = segment.end
                            }
                        )
                        ++counter
                    }

            }
        }
        counter
    }

}

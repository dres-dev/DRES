package dev.dres.api.rest.types.collection

import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.basics.media.MediaType

/**
 * The RESTful API equivalent for [MediaItem].
 *
 * @see MediaItem
 * @author Ralph Gasser
 * @version 1.1.0
 */
data class RestMediaItem(val id: String= UID.EMPTY.string, val name: String, val type: RestMediaItemType, val collectionId: String, val location: String, val durationMs: Long? = null, val fps: Float? = null) {
    companion object {
        /**
         * Generates a [RestMediaItem] from a [MediaItem] and returns it.
         *
         * @param item The [MediaItem] to convert.
         */
        fun fromMediaItem(item: MediaItem) = when (item.type) {
            MediaType.IMAGE -> RestMediaItem(item.id, item.name, RestMediaItemType.IMAGE, item.collection.id, item.location)
            MediaType.VIDEO -> RestMediaItem(item.id, item.name, RestMediaItemType.VIDEO, item.collection.id, item.location, item.durationMs, item.fps)
            else -> throw IllegalArgumentException("Unsupported media type ${item.type}.")
        }
    }

    init {
        if (this.type == RestMediaItemType.VIDEO) {
            require(this.durationMs != null) { "Duration must  be set for a video item." }
            require(this.fps != null) { "Duration must be set for a video item." }
        }
    }
}
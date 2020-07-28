package dres.api.rest.types.collection

import dres.data.dbo.DAO
import dres.data.model.basics.media.MediaItem
import dres.data.model.competition.TaskDescription
import dres.utilities.extensions.UID

/**
 * The RESTful API equivalent for [dres.data.model.basics.media.MediaItem].
 *
 * @see dres.data.model.basics.media.MediaItem
 * @author Ralph Gasser
 * @version 1.0
 */
data class RestMediaItem(val id: String, val name: String, val type: RestMediaItemType, val collectionId: String, val durationMs: Long? = null, val fps: Float? = null) {
    companion object {
        /**
         * Generates a [RestMediaItem] from a [TaskDescription] and returns it.
         *
         * @param task The [TaskDescription] to convert.
         */
        fun fromMediaItem(item: MediaItem) = when (item) {
            is MediaItem.ImageItem -> RestMediaItem(item.id.string, item.name, RestMediaItemType.IMAGE, item.collection.string)
            is MediaItem.VideoItem -> RestMediaItem(item.id.string, item.name,RestMediaItemType.VIDEO, item.collection.string, item.durationMs, item.fps)
        }
    }

    /**
     * Converts this [RestMediaItem] to the corresponding [MediaItem] and returns it,
     * by looking it up in the collection
     *
     * @param mediaItems The [DAO] to perform lookups
     */
    fun toMediaItem(mediaItems: DAO<MediaItem>) = mediaItems[this.id.UID()]
}
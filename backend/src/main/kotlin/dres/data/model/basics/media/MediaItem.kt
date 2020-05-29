package dres.data.model.basics.media

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import dres.data.model.Entity

/**
 * A media item such as a video or an image
 *
 * @author Ralph Gasser
 * @version 1.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "itemType")
@JsonSubTypes(
        JsonSubTypes.Type(value = MediaItem.ImageItem::class, name = "image"),
        JsonSubTypes.Type(value = MediaItem.VideoItem::class, name = "video")
)
sealed class MediaItem(val itemType: String) : Entity {

    abstract val collection: Long
    abstract val name: String
    abstract val location: String

    abstract fun withCollection(collection: Long): MediaItem

    data class ImageItem(override var id: Long, override val name: String, override val location: String, override val collection: Long): MediaItem("image") {
        override fun withCollection(collection: Long): ImageItem = ImageItem(id, name, location, collection)
    }

    data class VideoItem(override var id: Long, override val name: String, override val location: String, override val collection: Long, override val durationMs: Long, override val fps: Float): MediaItem("video"), PlayableMediaItem {
        override fun withCollection(collection: Long): VideoItem = VideoItem(id, name, location, collection, durationMs, fps)
    }
}
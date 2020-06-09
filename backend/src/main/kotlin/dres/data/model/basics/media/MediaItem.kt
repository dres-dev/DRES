package dres.data.model.basics.media

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
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


    data class ImageItem @JsonCreator constructor(
            @JsonProperty("id") override var id: Long,
            @JsonProperty("name") override val name: String,
            @JsonProperty("location") override val location: String,
            @JsonProperty("collection") override val collection: Long)
        : MediaItem("image") {
        override fun withCollection(collection: Long): ImageItem = ImageItem(id, name, location, collection)
    }

    data class VideoItem @JsonCreator constructor(
            @JsonProperty("id") override var id: Long,
            @JsonProperty("name") override val name: String,
            @JsonProperty("location") override val location: String,
            @JsonProperty("collection") override val collection: Long,
            @JsonProperty("durationMs") override val durationMs: Long,
            @JsonProperty("fps") override val fps: Float
    ): MediaItem("video"), PlayableMediaItem {
        override fun withCollection(collection: Long): VideoItem = VideoItem(id, name, location, collection, durationMs, fps)
    }
}
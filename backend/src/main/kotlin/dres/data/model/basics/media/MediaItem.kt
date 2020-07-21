package dres.data.model.basics.media

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import dres.data.model.Entity
import dres.data.model.UID

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

    abstract val collection: UID
    abstract val name: String
    abstract val location: String

    abstract fun withCollection(collection: UID): MediaItem


    data class ImageItem @JsonCreator constructor(
            @JsonProperty("id") override var id: UID,
            @JsonProperty("name") override val name: String,
            @JsonProperty("location") override val location: String,
            @JsonProperty("collection") override val collection: UID)
        : MediaItem("image") {
        override fun withCollection(collection: UID): ImageItem = ImageItem(id, name, location, collection)
    }

    data class VideoItem @JsonCreator constructor(
            @JsonProperty("id") override var id: UID,
            @JsonProperty("name") override val name: String,
            @JsonProperty("location") override val location: String,
            @JsonProperty("collection") override val collection: UID,
            @JsonProperty("durationMs") override val durationMs: Long,
            @JsonProperty("fps") override val fps: Float
    ): MediaItem("video"), PlayableMediaItem {
        override fun withCollection(collection: UID): VideoItem = VideoItem(id, name, location, collection, durationMs, fps)
    }
}
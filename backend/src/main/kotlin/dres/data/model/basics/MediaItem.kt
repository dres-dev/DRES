package dres.data.model.basics

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import dres.data.model.Entity
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonInput
import kotlinx.serialization.json.JsonObject
import java.time.Duration

/**
 * A media item such as a video or an image
 *
 * @author Ralph Gasser
 * @version 1.0
 */
@Serializable
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "itemType")
@JsonSubTypes(
        JsonSubTypes.Type(value = MediaItem.ImageItem::class, name = "image"),
        JsonSubTypes.Type(value = MediaItem.VideoItem::class, name = "video")
)
sealed class MediaItem(val itemType: String) : Entity {

    @Serializer(forClass = MediaItem::class)
    companion object : KSerializer<MediaItem> {

        override fun serialize(encoder: Encoder, value: MediaItem) {
            when(value) {
                is ImageItem -> encoder.encode(ImageItem.serializer(), value)
                is VideoItem -> encoder.encode(VideoItem.serializer(), value)
            }
        }

        //TODO: this is not as it should be but there are some strange deserialization issues...
        override fun deserialize(decoder: Decoder): MediaItem {

            val input = decoder as? JsonInput
                    ?: throw SerializationException("Expected JsonInput for ${decoder::class}")
            val jsonObject = input.decodeJson() as? JsonObject
                    ?: throw SerializationException("Expected JsonObject for ${input.decodeJson()::class}")

            val type = jsonObject.getPrimitive("itemType").content

            //automatic deserialization is broken, doing it manually...
            val id = jsonObject.getPrimitive("id").long
            val name = jsonObject.getPrimitive("name").content
            val location = jsonObject.getPrimitive("location").content
            val collection = jsonObject.getPrimitive("collection").long

            return when (type) {
                "image" -> ImageItem(id, name, location, collection)
                "video" -> VideoItem(id, name, location, collection, jsonObject.getPrimitive("ms").long, jsonObject.getPrimitive("fps").float)
                else -> throw SerializationException("Type $type not supported")
            }
        }

    }

    abstract val collection: Long
    abstract val name: String
    abstract val location: String

    abstract fun withCollection(collection: Long): MediaItem

    @Serializable
    data class ImageItem(override var id: Long, override val name: String, override val location: String, override val collection: Long): MediaItem("image") {

        override fun withCollection(collection: Long): ImageItem = ImageItem(id, name, location, collection)

    }

    @Serializable
    data class VideoItem(override var id: Long, override val name: String, override val location: String, override val collection: Long, val ms: Long, val fps: Float): MediaItem("video") {

        override fun withCollection(collection: Long): VideoItem = VideoItem(id, name, location, collection, ms, fps)

        fun duration(): Duration = Duration.ofMillis(ms)

    }
}
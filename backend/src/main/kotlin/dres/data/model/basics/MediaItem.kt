package dres.data.model.basics

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
sealed class MediaItem(val itemType: String) : Entity {



    @Serializer(forClass = MediaItem::class)
    companion object : KSerializer<MediaItem> {

        override fun serialize(encoder: Encoder, obj: MediaItem) {
            when(obj) {
                is ImageItem -> encoder.encode(ImageItem.serializer(), obj)
                is VideoItem -> encoder.encode(VideoItem.serializer(), obj)
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

    @Serializable
    data class ImageItem(override var id: Long, override val name: String, val location: String, override val collection: Long): MediaItem("image")

    @Serializable
    data class VideoItem(override var id: Long, override val name: String, val location: String, override val collection: Long, val ms: Long, val fps: Float): MediaItem("video") {

        fun duration(): Duration = Duration.ofMillis(ms)

    }
}
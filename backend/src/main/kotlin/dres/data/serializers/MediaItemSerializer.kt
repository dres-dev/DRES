package dres.data.serializers

import dres.data.model.basics.MediaItem
import dres.data.model.basics.MediaItem.Companion.IMAGE_MEDIA_ITEM
import dres.data.model.basics.MediaItem.Companion.VIDEO_MEDIA_ITEM
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer
import java.lang.IllegalStateException
import java.nio.file.Paths
import java.time.Duration

object MediaItemSerializer: Serializer<MediaItem> {
    override fun serialize(out: DataOutput2, value: MediaItem) = when (value) {
        is MediaItem.VideoItem -> {
            out.writeInt(VIDEO_MEDIA_ITEM)
            out.packLong(value.id)
            out.writeUTF(value.name)
            out.writeUTF(value.location.toString())
            out.packLong(value.duration.toMillis())
            out.writeFloat(value.fps)
        }
        is MediaItem.ImageItem -> {
            out.writeInt(IMAGE_MEDIA_ITEM)
            out.packLong(value.id)
            out.writeUTF(value.name)
            out.writeUTF(value.location.toString())
        }
    }

    override fun deserialize(input: DataInput2, available: Int): MediaItem = when (input.readInt()) {
        VIDEO_MEDIA_ITEM -> MediaItem.VideoItem(input.unpackLong(), input.readUTF(), Paths.get(input.readUTF()), Duration.ofMillis(input.unpackLong()), input.readFloat())
        IMAGE_MEDIA_ITEM -> MediaItem.ImageItem(input.unpackLong(), input.readUTF(), Paths.get(input.readUTF()))
        else -> throw IllegalStateException("Unsupported MediaItem type detected upon deserialization.")
    }
}
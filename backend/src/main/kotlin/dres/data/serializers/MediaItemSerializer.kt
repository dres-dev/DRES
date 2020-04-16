package dres.data.serializers

import dres.data.model.basics.media.MediaItem

import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer
import java.lang.IllegalStateException


object MediaItemSerializer: Serializer<MediaItem> {
    override fun serialize(out: DataOutput2, value: MediaItem) = when (value) {
        is MediaItem.VideoItem -> {
            out.writeInt(0)
            out.packLong(value.id)
            out.writeUTF(value.name)
            out.writeUTF(value.location)
            out.packLong(value.collection)
            out.packLong(value.durationMs)
            out.writeFloat(value.fps)
        }
        is MediaItem.ImageItem -> {
            out.writeInt(1)
            out.packLong(value.id)
            out.writeUTF(value.name)
            out.writeUTF(value.location)
            out.packLong(value.collection)
        }
    }

    override fun deserialize(input: DataInput2, available: Int): MediaItem = when (input.readInt()) {
        0 -> MediaItem.VideoItem(input.unpackLong(), input.readUTF(), input.readUTF(), input.unpackLong(), input.unpackLong(), input.readFloat())
        1 -> MediaItem.ImageItem(input.unpackLong(), input.readUTF(), input.readUTF(), input.unpackLong())
        else -> throw IllegalStateException("Unsupported MediaItem type detected upon deserialization.")
    }
}
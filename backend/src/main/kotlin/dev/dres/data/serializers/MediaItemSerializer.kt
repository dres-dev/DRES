package dev.dres.data.serializers

import dev.dres.data.model.basics.media.MediaItem
import dev.dres.utilities.extensions.readUID
import dev.dres.utilities.extensions.writeUID

import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer
import java.lang.IllegalStateException


object MediaItemSerializer: Serializer<MediaItem> {
    override fun serialize(out: DataOutput2, value: MediaItem) = when (value) {
        is MediaItem.VideoItem -> {
            out.writeInt(0)
            out.writeUID(value.id)
            out.writeUTF(value.name)
            out.writeUTF(value.location)
            out.writeUID(value.collection)
            out.packLong(value.durationMs)
            out.writeFloat(value.fps)
        }
        is MediaItem.ImageItem -> {
            out.writeInt(1)
            out.writeUID(value.id)
            out.writeUTF(value.name)
            out.writeUTF(value.location)
            out.writeUID(value.collection)
        }
    }

    override fun deserialize(input: DataInput2, available: Int): MediaItem {
        val i = input.readInt()
        return when (i) {
            0 -> MediaItem.VideoItem(input.readUID(), input.readUTF(), input.readUTF(), input.readUID(), input.unpackLong(), input.readFloat())
            1 -> MediaItem.ImageItem(input.readUID(), input.readUTF(), input.readUTF(), input.readUID())
            else -> throw IllegalStateException("Unsupported MediaItem $i type detected upon deserialization.")
        }
    }
}
package dev.dres.data.serializers

import dev.dres.data.model.basics.media.MediaCollection
import dev.dres.utilities.extensions.readUID
import dev.dres.utilities.extensions.writeUID
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object MediaCollectionSerializer: Serializer<MediaCollection> {
    override fun serialize(out: DataOutput2, value: MediaCollection) {
        out.writeUID(value.id)
        out.writeUTF(value.name)
        out.writeUTF(value.description ?: "")
        out.writeUTF(value.path)
    }

    override fun deserialize(input: DataInput2, available: Int): MediaCollection {
        return MediaCollection(input.readUID(), input.readUTF(), input.readUTF(), input.readUTF())
    }
}
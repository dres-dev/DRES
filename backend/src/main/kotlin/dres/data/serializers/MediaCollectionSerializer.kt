package dres.data.serializers

import dres.data.model.basics.media.MediaCollection
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object MediaCollectionSerializer: Serializer<MediaCollection> {
    override fun serialize(out: DataOutput2, value: MediaCollection) {
        out.packLong(value.id)
        out.writeUTF(value.name)
        out.writeUTF(value.description ?: "")
        out.writeUTF(value.basePath)
    }

    override fun deserialize(input: DataInput2, available: Int): MediaCollection {
        return MediaCollection(input.unpackLong(), input.readUTF(), input.readUTF(), input.readUTF())
    }
}
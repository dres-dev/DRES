package dres.data.serializers

import dres.data.model.UID
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object UIDSerializer : Serializer<UID> {
    override fun serialize(out: DataOutput2, value: UID) {
        out.writeUTF(value.string)
    }

    override fun deserialize(input: DataInput2, available: Int): UID {
        return UID(input.readUTF())
    }
}
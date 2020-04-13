package dres.data.serializers

import dres.data.model.run.Submission
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object SubmissionSerializer : Serializer<Submission> {
    override fun serialize(out: DataOutput2, value: Submission) {
        out.packInt(value.team)
        out.packInt(value.tool)
        out.packLong(value.timestamp)
        out.writeUTF(value.collection)
        out.writeUTF(value.item)
        out.packLong(value.start ?: -1L)
        out.packLong(value.end ?: -1L)
    }

    override fun deserialize(input: DataInput2, available: Int): Submission = Submission(
            input.unpackInt(),
            input.unpackInt(),
            input.unpackLong(),
            input.readUTF(),
            input.readUTF(),
            input.unpackLong().let { if (it == -1L) { null } else { it } },
            input.unpackLong().let { if (it == -1L) { null } else { it } }
    )
}
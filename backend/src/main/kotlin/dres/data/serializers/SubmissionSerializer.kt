package dres.data.serializers

import dres.data.model.run.Submission
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object SubmissionSerializer : Serializer<Submission> {
    override fun serialize(out: DataOutput2, value: Submission) {
        out.packInt(value.team)
        out.packLong(value.member)
        out.packLong(value.timestamp)
        MediaItemSerializer.serialize(out, value.item)
        out.packLong(value.start ?: -1L)
        out.packLong(value.end ?: -1L)
        out.writeUTF(value.id)
    }

    override fun deserialize(input: DataInput2, available: Int): Submission = Submission(
        input.unpackInt(),
        input.unpackLong(),
        input.unpackLong(),
        MediaItemSerializer.deserialize(input, available),
        input.unpackLong().let { if (it == -1L) { null } else { it } },
        input.unpackLong().let { if (it == -1L) { null } else { it } },
        input.readUTF()
    )
}
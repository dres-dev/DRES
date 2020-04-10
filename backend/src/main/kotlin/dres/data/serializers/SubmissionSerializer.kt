package dres.data.serializers

import dres.data.model.run.Submission
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object SubmissionSerializer : Serializer<Submission> {
    override fun serialize(out: DataOutput2, value: Submission) {
        out.writeInt(value.team)
        out.writeLong(value.timestamp)
        out.writeUTF(value.collection)
        out.writeUTF(value.item)
        out.writeLong(value.start ?: -1L)
        out.writeLong(value.end ?: -1L)
    }

    override fun deserialize(input: DataInput2, available: Int): Submission = Submission(
            input.readInt(),
            input.readLong(),
            input.readUTF(),
            input.readUTF(),
            input.readLong().let { if (it == -1L) { null } else { it } },
            input.readLong().let { if (it == -1L) { null } else { it } }
    )
}
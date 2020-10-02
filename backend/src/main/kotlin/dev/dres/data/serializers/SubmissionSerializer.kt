package dev.dres.data.serializers

import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.utilities.extensions.readUID
import dev.dres.utilities.extensions.writeUID
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object SubmissionSerializer : Serializer<Submission> {
    override fun serialize(out: DataOutput2, value: Submission) {
        out.packInt(value.team)
        out.writeUID(value.member)
        out.packLong(value.timestamp)
        MediaItemSerializer.serialize(out, value.item)
        out.packLong(value.start ?: -1L)
        out.packLong(value.end ?: -1L)
        out.writeUTF(value.uid)
        out.packInt(value.status.ordinal)
    }

    override fun deserialize(input: DataInput2, available: Int): Submission = Submission(
        input.unpackInt(),
        input.readUID(),
        input.unpackLong(),
        MediaItemSerializer.deserialize(input, available),
        input.unpackLong().let { if (it == -1L) { null } else { it } },
        input.unpackLong().let { if (it == -1L) { null } else { it } },
        input.readUTF()
    ).apply { this.status = SubmissionStatus.values()[input.unpackInt()] }
}
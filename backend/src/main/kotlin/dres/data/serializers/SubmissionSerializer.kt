package dres.data.serializers

import dres.data.model.run.AvsSubmission
import dres.data.model.run.KisSubmission
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionType

import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer
import java.lang.IllegalStateException

object SubmissionSerializer : Serializer<Submission> {
    override fun serialize(out: DataOutput2, value: Submission) {
        out.writeInt(value.type.ordinal)
        out.writeInt(value.team)
        out.writeLong(value.timestamp)
        out.writeUTF(value.name)
        when (value) {
            is AvsSubmission -> { /* Nothing. */ }
            is KisSubmission -> {
                out.writeLong(value.start)
                out.writeLong(value.end)
            }
        }
    }

    override fun deserialize(input: DataInput2, available: Int): Submission = when(input.readInt()) {
        SubmissionType.AVS.ordinal -> AvsSubmission(input.readInt(), input.readLong(), input.readUTF())
        SubmissionType.KIS.ordinal -> KisSubmission(input.readInt(), input.readLong(), input.readUTF(), input.readLong(), input.readLong())
        else -> throw IllegalStateException("Unsupported Submission type detected upon deserialization.")
    }
}
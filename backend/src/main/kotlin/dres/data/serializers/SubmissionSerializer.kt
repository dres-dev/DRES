package dres.data.serializers

import dres.data.model.run.LSCSubmission
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionType
import dres.data.model.run.VBSSubmission
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object SubmissionSerializer : Serializer<Submission> {
    override fun serialize(out: DataOutput2, value: Submission) {
        out.writeInt(value.type.ordinal)
        out.writeInt(value.team)
        out.writeLong(value.timestamp)
        out.writeUTF(value.collection)
        out.writeUTF(value.item)
        when (value) {
            is LSCSubmission -> { /* Nothing. */ }
            is VBSSubmission -> {
                out.writeLong(value.start)
                out.writeLong(value.end)
            }
        }
    }

    override fun deserialize(input: DataInput2, available: Int): Submission = when(input.readInt()) {
        SubmissionType.LSC.ordinal -> LSCSubmission(input.readInt(), input.readLong(), input.readUTF(), input.readUTF())
        SubmissionType.VBS.ordinal -> VBSSubmission(input.readInt(), input.readLong(), input.readUTF(), input.readUTF(), input.readLong(), input.readLong())
        else -> throw IllegalStateException("Unsupported Submission type detected upon deserialization.")
    }
}
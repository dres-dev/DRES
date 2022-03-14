package dev.dres.data.serializers


import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.utilities.extensions.readUID
import dev.dres.utilities.extensions.writeUID
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object SubmissionSerializer : Serializer<Submission> {
    override fun serialize(out: DataOutput2, value: Submission) {
        out.writeUID(value.uid)
        out.writeUID(value.teamId)
        out.writeUID(value.memberId)
        out.packLong(value.timestamp)

        out.packInt(value.status.ordinal)
        when(value){
            is Submission.Item -> {
                out.packInt(0)
                MediaItemSerializer.serialize(out, value.item)
            }
            is Submission.Temporal -> {
                out.packInt(1)
                MediaItemSerializer.serialize(out, value.item)
                out.packLong(value.start)
                out.packLong(value.end)
            }
            is Submission.Text -> {
                out.packInt(2)
                out.writeUTF(value.text)
            }
        }
    }

    override fun deserialize(input: DataInput2, available: Int): Submission {
        val id = input.readUID()
        val teamId = input.readUID()
        val memberId = input.readUID()
        val timestamp = input.unpackLong()
        val status = SubmissionStatus.values()[input.unpackInt()]

        return when(input.unpackInt()) {
            0 -> {
                val item = MediaItemSerializer.deserialize(input, available)
                Submission.Item(teamId, memberId, timestamp, item, id).apply { this.status = status }
            }
            1 -> {
                val item = MediaItemSerializer.deserialize(input, available)
                Submission.Temporal(teamId, memberId, timestamp, item, input.unpackLong(), input.unpackLong(), id).apply { this.status = status }
            }
            2 -> Submission.Text(teamId, memberId, timestamp, input.readUTF(), id).apply { this.status = status }
            else -> throw IllegalStateException("Unknown Submission Type")
        }
    }
}
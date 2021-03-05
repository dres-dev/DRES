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
        MediaItemSerializer.serialize(out, value.item)
        out.packInt(value.status.ordinal)
        when(value){
            is Submission.Item -> out.packInt(0)
            is Submission.Temporal -> {
                out.packInt(1)
                out.packLong(value.start)
                out.packLong(value.end)
            }
        }
    }

    override fun deserialize(input: DataInput2, available: Int): Submission {
        val id = input.readUID()
        val teamId = input.readUID()
        val memberId = input.readUID()
        val timestamp = input.unpackLong()
        val item = MediaItemSerializer.deserialize(input, available)
        val status = SubmissionStatus.values()[input.unpackInt()]

        return when(input.unpackInt()) {
            0 -> Submission.Item(teamId, memberId, timestamp, item, id).apply { this.status = status }
            1 -> Submission.Temporal(teamId, memberId, timestamp, item, input.unpackLong(), input.unpackLong(), id).apply { this.status = status }
            else -> throw IllegalStateException("Unknown Submission Type")
        }
    }
}
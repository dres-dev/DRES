package dev.dres.data.serializers

import dev.dres.data.model.run.ItemSubmission
import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.data.model.run.TemporalSubmission
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
        out.packInt(value.status.ordinal)
        out.writeUTF(value.uid)

        when(value){
            is ItemSubmission -> {
                out.packInt(0)
            }
            is TemporalSubmission -> {
                out.packInt(1)
                out.packLong(value.start)
                out.packLong(value.end)
            }
        }

    }

    override fun deserialize(input: DataInput2, available: Int): Submission {

        val team = input.unpackInt()
        val member = input.readUID()
        val timestamp = input.unpackLong()
        val item = MediaItemSerializer.deserialize(input, available)
        val status = SubmissionStatus.values()[input.unpackInt()]
        val id = input.readUTF()

        return when(input.unpackInt()) {
            0 -> ItemSubmission(team, member, timestamp, item, id).apply { this.status = status }
            1 -> TemporalSubmission(team, member, timestamp, item,
                    input.unpackLong(),
                    input.unpackLong(),
                    id
                ).apply { this.status = status }
            else -> throw IllegalStateException("Unknown Submission Type")
        }
    }
}
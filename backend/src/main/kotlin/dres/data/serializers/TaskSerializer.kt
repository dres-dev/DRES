package dres.data.serializers

import dres.data.model.competition.Task
import dres.data.model.competition.TaskType
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object TaskSerializer: Serializer<Task> {
    override fun serialize(out: DataOutput2, value: Task) {
        out.packLong(value.id)
        out.writeUTF(value.name)
        out.writeInt(value.type.ordinal)
        out.writeBoolean(value.novice)
        TaskDescriptionSerializer.serialize(out, value.description)
    }

    override fun deserialize(input: DataInput2, available: Int): Task = Task(
        input.unpackLong(),
        input.readUTF(),
        TaskType.values()[input.readInt()],
        input.readBoolean(),
        TaskDescriptionSerializer.deserialize(input, available)
    )
}
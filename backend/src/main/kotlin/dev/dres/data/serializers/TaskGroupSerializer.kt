package dev.dres.data.serializers

import dev.dres.data.model.competition.TaskGroup
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object TaskGroupSerializer: Serializer<TaskGroup> {
    override fun serialize(out: DataOutput2, value: TaskGroup) {
        out.writeUTF(value.name)
        out.writeUTF(value.type)
    }
    override fun deserialize(input: DataInput2, available: Int): TaskGroup = TaskGroup(input.readUTF(), input.readUTF())
}
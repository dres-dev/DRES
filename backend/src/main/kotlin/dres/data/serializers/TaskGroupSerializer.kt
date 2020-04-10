package dres.data.serializers

import dres.data.model.competition.TaskGroup
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object TaskGroupSerializer: Serializer<TaskGroup> {
    override fun serialize(out: DataOutput2, value: TaskGroup) {
        out.writeUTF(value.name)
        out.packLong(value.defaultTaskDuration)
    }

    override fun deserialize(input: DataInput2, available: Int): TaskGroup = TaskGroup(input.readUTF(), input.unpackLong())
}
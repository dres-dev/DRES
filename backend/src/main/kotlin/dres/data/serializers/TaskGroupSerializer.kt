package dres.data.serializers

import dres.data.model.competition.TaskGroup
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object TaskGroupSerializer: Serializer<TaskGroup> {
    override fun serialize(out: DataOutput2, value: TaskGroup) { //FIXME
//        out.writeUTF(value.name)
//        out.writeInt(value.type.ordinal)
//        out.packLong(value.defaultTaskDuration)
    }
    override fun deserialize(input: DataInput2, available: Int): TaskGroup { //FIXME

//        return TaskGroup(input.readUTF(), TaskType.values()[input.readInt()], input.unpackLong())

        TODO()

    }
}
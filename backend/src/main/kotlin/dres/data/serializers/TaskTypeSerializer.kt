package dres.data.serializers

import dres.data.model.competition.TaskType
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object TaskTypeSerializer : Serializer<TaskType> {

    override fun serialize(out: DataOutput2, value: TaskType) {
        out.writeUTF(value.name)
        out.packLong(value.taskDuration)
        out.writeUTF(value.targetType.name)
        out.packInt(value.components.size)
        value.components.forEach { out.writeUTF(it.name) }
        out.writeUTF(value.score.name)
        out.packInt(value.filter.size)
        value.filter.forEach { out.writeUTF(it.name) }
        out.packInt(value.options.size)
        value.options.forEach { out.writeUTF(it.name) }
    }

    override fun deserialize(input: DataInput2, available: Int): TaskType =
        TaskType(
                input.readUTF(),
                input.unpackLong(),
                TaskType.TargetType.valueOf(input.readUTF()),
                (0 until input.unpackInt()).map {TaskType.QueryComponentType.valueOf(input.readUTF())}.toSet(),
                TaskType.ScoringType.valueOf(input.readUTF()),
                (0 until input.unpackInt()).map {TaskType.SubmissionFilterType.valueOf(input.readUTF())}.toSet(),
                (0 until input.unpackInt()).map {TaskType.Options.valueOf(input.readUTF())}.toSet()
        )
}
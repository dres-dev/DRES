package dres.data.serializers

import dres.data.model.competition.TaskType
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object TaskTypeSerializer : Serializer<TaskType> {

    override fun serialize(out: DataOutput2, value: TaskType) {
        out.writeUTF(value.name)
        out.packLong(value.taskDuration)
        out.packInt(value.targetType.ordinal)
        out.packInt(value.components.size)
        value.components.forEach { out.packInt(it.ordinal) }
        out.packInt(value.score.ordinal)
        out.packInt(value.filter.size)
        value.filter.forEach { out.packInt(it.ordinal) }
        out.packInt(value.options.size)
        value.options.forEach { out.packInt(it.ordinal) }
    }

    override fun deserialize(input: DataInput2, available: Int): TaskType =
        TaskType(
            input.readUTF(),
            input.unpackLong(),
            TaskType.TargetType.values()[input.unpackInt()],
            (0 until input.unpackInt()).map { TaskType.QueryComponentType.values()[input.unpackInt()] }.toSet(),
            TaskType.ScoringType.values()[input.unpackInt()],
            (0 until input.unpackInt()).map { TaskType.SubmissionFilterType.values()[input.unpackInt()] }.toSet(),
            (0 until input.unpackInt()).map { TaskType.Options.values()[input.unpackInt()] }.toSet()
        )
}
package dres.data.serializers

import dres.data.model.basics.MediaItem
import dres.data.model.competition.TaskDescriptionBase
import dres.data.model.competition.TaskType
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer
import java.lang.IllegalStateException

object TaskDescriptionSerializer: Serializer<TaskDescriptionBase> {
    override fun serialize(out: DataOutput2, value: TaskDescriptionBase) = when (value) {
        is TaskDescriptionBase.KisVisualTaskDescription -> {
            out.writeInt(value.taskType.ordinal)
            MediaItemSerializer.serialize(out, value.item)
            TemporalRangeSerializer.serialize(out, value.temporalRange)
        }
        is TaskDescriptionBase.KisTextualTaskDescription -> {
            out.writeInt(value.taskType.ordinal)
            MediaItemSerializer.serialize(out, value.item)
            TemporalRangeSerializer.serialize(out, value.temporalRange)
            out.writeInt(value.descriptions.size)
            for (description in value.descriptions) {
                out.writeUTF(description)
            }
            out.writeInt(value.delay)
        }
        is TaskDescriptionBase.AvsTaskDescription -> {
            out.writeInt(value.taskType.ordinal)
            out.writeUTF(value.description)
        }
    }

    override fun deserialize(input: DataInput2, available: Int): TaskDescriptionBase = when (input.readInt()) {
        TaskType.KIS_VISUAL.ordinal -> TaskDescriptionBase.KisVisualTaskDescription(MediaItemSerializer.deserialize(input, available) as MediaItem.VideoItem, TemporalRangeSerializer.deserialize(input, available))
        TaskType.KIS_TEXTUAL.ordinal -> TaskDescriptionBase.KisTextualTaskDescription(MediaItemSerializer.deserialize(input, available) as MediaItem.VideoItem, TemporalRangeSerializer.deserialize(input, available), (0 until input.readInt()).map { input.readUTF() }, input.readInt())
        TaskType.AVS.ordinal -> TaskDescriptionBase.AvsTaskDescription(input.readUTF())
        else -> throw IllegalStateException("Unsupported TaskDescription type detected upon deserialization.")
    }
}
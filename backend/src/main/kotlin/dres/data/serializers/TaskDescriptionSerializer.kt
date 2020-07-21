package dres.data.serializers

import dres.data.model.basics.media.MediaItem
import dres.data.model.competition.TaskDescriptionBase
import dres.data.model.competition.TaskType
import dres.utilities.extensions.readUID
import dres.utilities.extensions.writeUID
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object TaskDescriptionSerializer: Serializer<TaskDescriptionBase> {
    override fun serialize(out: DataOutput2, value: TaskDescriptionBase) {
        out.writeUTF(value.uid)
        out.writeUTF(value.name)
        TaskGroupSerializer.serialize(out, value.taskGroup)
        out.packLong(value.duration)
        when (value) {
            is TaskDescriptionBase.KisVisualTaskDescription -> {
                MediaItemSerializer.serialize(out, value.item)
                TemporalRangeSerializer.serialize(out, value.temporalRange)
            }
            is TaskDescriptionBase.KisTextualTaskDescription -> {
                MediaItemSerializer.serialize(out, value.item)
                TemporalRangeSerializer.serialize(out, value.temporalRange)
                out.writeInt(value.descriptions.size)
                for (description in value.descriptions) {
                    out.writeUTF(description)
                }
                out.writeInt(value.delay)
            }
            is TaskDescriptionBase.AvsTaskDescription -> {
                out.writeUTF(value.description)
                out.writeUID(value.defaultCollection)
            }
        }
    }

    override fun deserialize(input: DataInput2, available: Int): TaskDescriptionBase {
        val uid = input.readUTF()
        val name = input.readUTF()
        val taskGroup = TaskGroupSerializer.deserialize(input, available)
        val duration = input.unpackLong()
        return when (taskGroup.type) {
            TaskType.KIS_VISUAL-> TaskDescriptionBase.KisVisualTaskDescription(uid, name, taskGroup, duration, MediaItemSerializer.deserialize(input, available) as MediaItem.VideoItem, TemporalRangeSerializer.deserialize(input, available))
            TaskType.KIS_TEXTUAL -> TaskDescriptionBase.KisTextualTaskDescription(uid, name, taskGroup, duration, MediaItemSerializer.deserialize(input, available) as MediaItem.VideoItem, TemporalRangeSerializer.deserialize(input, available), (0 until input.readInt()).map { input.readUTF() }, input.readInt())
            TaskType.AVS -> TaskDescriptionBase.AvsTaskDescription(uid, name, taskGroup, duration, input.readUTF(), input.readUID())
        }
    }
}
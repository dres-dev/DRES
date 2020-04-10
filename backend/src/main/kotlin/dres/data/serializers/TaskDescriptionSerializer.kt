package dres.data.serializers

import dres.data.model.basics.MediaItem
import dres.data.model.competition.AvsTaskDescription
import dres.data.model.competition.KisTextualTaskDescription
import dres.data.model.competition.KisVisualTaskDescription
import dres.data.model.competition.interfaces.TaskDescription
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object TaskDescriptionSerializer: Serializer<TaskDescription> {
    override fun serialize(out: DataOutput2, value: TaskDescription){
        out.writeUTF(value::class.simpleName!!)
        out.writeUTF(value.name)
        TaskGroupSerializer.serialize(out, value.taskGroup)
        out.packLong(value.duration)
        when (value) {
            is KisVisualTaskDescription -> {
                MediaItemSerializer.serialize(out, value.item)
                TemporalRangeSerializer.serialize(out, value.temporalRange)
            }
            is KisTextualTaskDescription -> {
                MediaItemSerializer.serialize(out, value.item)
                TemporalRangeSerializer.serialize(out, value.temporalRange)
                out.writeInt(value.descriptions.size)
                for (description in value.descriptions) {
                    out.writeUTF(description)
                }
                out.writeInt(value.delay)
            }
            is AvsTaskDescription -> {
                out.writeUTF(value.description)
            }
        }
    }

    override fun deserialize(input: DataInput2, available: Int): TaskDescription{
        val className = input.readUTF()
        val name = input.readUTF()
        val taskGroup = TaskGroupSerializer.deserialize(input, available)
        val duration = input.unpackLong()
        return when (className) {
            KisVisualTaskDescription::class.simpleName -> KisVisualTaskDescription(name, taskGroup, duration, MediaItemSerializer.deserialize(input, available) as MediaItem.VideoItem, TemporalRangeSerializer.deserialize(input, available))
            KisTextualTaskDescription::class.simpleName -> KisTextualTaskDescription(name, taskGroup, duration, MediaItemSerializer.deserialize(input, available) as MediaItem.VideoItem, TemporalRangeSerializer.deserialize(input, available), (0 until input.readInt()).map { input.readUTF() }, input.readInt())
            AvsTaskDescription::class.simpleName -> AvsTaskDescription(name, taskGroup, duration, input.readUTF())
            else -> throw IllegalStateException("Unsupported TaskDescription type detected upon deserialization.")
        }
    }
}
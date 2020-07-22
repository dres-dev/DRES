package dres.data.serializers

import dres.data.model.competition.interfaces.TaskDescription
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object TaskDescriptionSerializer: Serializer<TaskDescription> {
    override fun serialize(out: DataOutput2, value: TaskDescription) { //FIXME
//        out.writeUTF(value.uid)
//        out.writeUTF(value.name)
//        TaskGroupSerializer.serialize(out, value.taskGroup)
//        out.packLong(value.duration)
//        when (value) {
//            is KisVisualTaskDescription -> {
//                MediaItemSerializer.serialize(out, value.item)
//                TemporalRangeSerializer.serialize(out, value.temporalRange)
//            }
//            is KisTextualTaskDescription -> {
//                MediaItemSerializer.serialize(out, value.item)
//                TemporalRangeSerializer.serialize(out, value.temporalRange)
//                out.writeInt(value.descriptions.size)
//                for (description in value.descriptions) {
//                    out.writeUTF(description)
//                }
//                out.writeInt(value.delay)
//            }
//            is AvsTaskDescription -> {
//                out.writeUTF(value.description)
//                out.packLong(value.defaultCollection)
//            }
//        }
    }

    override fun deserialize(input: DataInput2, available: Int): TaskDescription { //FIXME
//        val uid = input.readUTF()
//        val name = input.readUTF()
//        val taskGroup = TaskGroupSerializer.deserialize(input, available)
//        val duration = input.unpackLong()
//        return when (taskGroup.type) {
//            TaskType.KIS_VISUAL-> KisVisualTaskDescription(uid, name, taskGroup, duration, MediaItemSerializer.deserialize(input, available) as MediaItem.VideoItem, TemporalRangeSerializer.deserialize(input, available))
//            TaskType.KIS_TEXTUAL -> KisTextualTaskDescription(uid, name, taskGroup, duration, MediaItemSerializer.deserialize(input, available) as MediaItem.VideoItem, TemporalRangeSerializer.deserialize(input, available), (0 until input.readInt()).map { input.readUTF() }, input.readInt())
//            TaskType.AVS -> AvsTaskDescription(uid, name, taskGroup, duration, input.readUTF(), input.unpackLong())
//        }

        TODO()

    }
}
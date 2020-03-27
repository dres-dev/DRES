package dres.data.serializers

import dres.data.model.basics.MediaItem
import dres.data.model.competition.TaskDescription
import dres.data.model.competition.TaskDescription.Companion.AVS_TASK_DESCRIPTION
import dres.data.model.competition.TaskDescription.Companion.KIS_TEXTUAL_TASK_DESCRIPTION
import dres.data.model.competition.TaskDescription.Companion.KIS_VISUAL_TASK_DESCRIPTION
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer
import java.lang.IllegalStateException

object TaskDescriptionSerializer: Serializer<TaskDescription> {
    override fun serialize(out: DataOutput2, value: TaskDescription) = when (value) {
        is TaskDescription.KisVisualTaskDescription -> {
            out.writeInt(KIS_VISUAL_TASK_DESCRIPTION)
            MediaItemSerializer.serialize(out, value.item)
            TemporalRangeSerializer.serialize(out, value.temporalRange)
        }
        is TaskDescription.KisTextualTaskDescription -> {
            out.writeInt(KIS_TEXTUAL_TASK_DESCRIPTION)
            MediaItemSerializer.serialize(out, value.item)
            TemporalRangeSerializer.serialize(out, value.temporalRange)
            out.writeInt(value.descriptions.size)
            for (description in value.descriptions) {
                out.writeUTF(description)
            }
            out.writeInt(value.delay)
        }
        is TaskDescription.AvsTaskDescription -> {
            out.writeInt(AVS_TASK_DESCRIPTION)
            out.writeUTF(value.description)
        }
    }

    override fun deserialize(input: DataInput2, available: Int): TaskDescription = when (input.readInt()) {
        KIS_VISUAL_TASK_DESCRIPTION -> TaskDescription.KisVisualTaskDescription(MediaItemSerializer.deserialize(input, available) as MediaItem.VideoItem, TemporalRangeSerializer.deserialize(input, available))
        KIS_TEXTUAL_TASK_DESCRIPTION -> TaskDescription.KisTextualTaskDescription(MediaItemSerializer.deserialize(input, available) as MediaItem.VideoItem, TemporalRangeSerializer.deserialize(input, available), (0 until input.readInt()).map { input.readUTF() }, input.readInt())
        AVS_TASK_DESCRIPTION -> TaskDescription.AvsTaskDescription(input.readUTF())
        else -> throw IllegalStateException("Unsupported TaskDescription type detected upon deserialization.")
    }
}
package dev.dres.data.serializers

import dev.dres.data.dbo.DAO
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.competition.*
import dev.dres.utilities.extensions.readUID
import dev.dres.utilities.extensions.writeUID
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer
import java.nio.file.Paths

class TaskDescriptionSerializer(val taskGroups: List<TaskGroup>, val taskTypes: List<TaskType>, val mediaItems: DAO<MediaItem>): Serializer<TaskDescription> {

    /**
     * Serializes [TaskDescription]
     */
    override fun serialize(out: DataOutput2, value: TaskDescription) {
        out.writeUID(value.id)
        out.writeUTF(value.name)
        out.writeUTF(value.taskGroup.name)
        out.writeUTF(value.taskType.name)
        out.packLong(value.duration)
        out.writeUID(value.mediaCollectionId)
        writeTaskDescriptionTarget(out, value.target)
        writeTaskDescriptionHints(out, value.hints)
    }

    /**
     * Deserializes [TaskDescription]
     */
    override fun deserialize(input: DataInput2, available: Int): TaskDescription = TaskDescription(
            input.readUID(),
            input.readUTF(),
            input.readUTF().let { n -> taskGroups.first { it.name == n } },
            input.readUTF().let { n -> taskTypes.first { it.name == n } },
            input.unpackLong(),
            input.readUID(),
            readTaskDescriptionTarget(input, available, this.mediaItems),
            readTaskDescriptionComponents(input, available, this.mediaItems)
    )

    /**
     * Part of serialization of [TaskDescription]. Writes [TaskDescriptionTarget]
     *
     * @param out [DataOutput2] to write to.
     * @param out [TaskDescriptionTarget] to serialize.
     */
    private fun writeTaskDescriptionTarget(out: DataOutput2, target: TaskDescriptionTarget) {
        out.packInt(target.ordinal)
        when(target) {
            is TaskDescriptionTarget.JudgementTaskDescriptionTarget -> {
                out.packInt(target.targets.size)
                target.targets.forEach {
                    out.writeUID(it.first.id)
                    out.writeBoolean(it.second != null)
                    if (it.second != null){
                        TemporalRangeSerializer.serialize(out, it.second!!)
                    }
                }
            }
            is TaskDescriptionTarget.VideoSegmentTarget -> {
                out.writeUID(target.item.id)
                TemporalRangeSerializer.serialize(out, target.temporalRange)
            }
            is TaskDescriptionTarget.MediaItemTarget -> {
                out.writeUID(target.item.id)
            }
            is TaskDescriptionTarget.MultipleMediaItemTarget -> {
                out.packInt(target.items.size)
                target.items.forEach { out.writeUID(it.id) }
            }
        }
    }

    /**
     * Part of serialization of [TaskDescription]. Writes [TaskDescriptionHint]s
     *
     * @param out [DataOutput2] to write to.
     * @param out [TaskDescriptionHint]s to serialize.
     */
    private fun writeTaskDescriptionHints(out: DataOutput2, hints: List<TaskDescriptionHint>){
        out.packInt(hints.size)
        hints.forEach {
            out.packLong(it.start ?: -1L)
            out.packLong(it.end ?: -1L)
            out.packInt(it.ordinal)
            when(it) {
                is TaskDescriptionHint.TextTaskDescriptionHint -> {
                    out.writeUTF(it.text)
                }
                is TaskDescriptionHint.VideoItemSegmentTaskDescriptionHint -> {
                    out.writeUID(it.item.id)
                    TemporalRangeSerializer.serialize(out, it.temporalRange)
                }
                is TaskDescriptionHint.ImageItemTaskDescriptionHint -> {
                    out.writeUID(it.item.id)
                }
                is TaskDescriptionHint.ExternalImageTaskDescriptionHint -> {
                    out.writeUTF(it.imageLocation.toString())
                }
                is TaskDescriptionHint.ExternalVideoTaskDescriptionHint -> {
                    out.writeUTF(it.videoLocation.toString())
                }
            }
        }
    }

    /**
     * Part of deserialization of [TaskDescription]. Reads [TaskDescriptionTarget]
     *
     * @param input [DataInput2] to read from.
     * @param mediaItems [DAO] to lookup [MediaItem]s
     *
     * @return Deserialized [TaskDescriptionTarget].
     */
    private fun readTaskDescriptionTarget(input: DataInput2, available: Int, mediaItems: DAO<MediaItem>) : TaskDescriptionTarget {
        return when(val ordinal = input.unpackInt()) {
            1 -> TaskDescriptionTarget.JudgementTaskDescriptionTarget(
                (0 until input.unpackInt()).map {
                    Pair(mediaItems[input.readUID()]!!, if (input.readBoolean()) {
                        TemporalRangeSerializer.deserialize(input, available)
                    } else null)
                }
            )
            2 -> TaskDescriptionTarget.MediaItemTarget(mediaItems[input.readUID()]!!)
            3 -> TaskDescriptionTarget.VideoSegmentTarget(mediaItems[input.readUID()]!! as MediaItem.VideoItem, TemporalRangeSerializer.deserialize(input, available))
            4 -> TaskDescriptionTarget.MultipleMediaItemTarget((0 until input.unpackInt()).map { mediaItems[input.readUID()]!! })
            else -> throw IllegalStateException("Failed to deserialize Task Description Target for ordinal $ordinal; not implemented.")
        }
    }

    /**
     * Part of deserialization of [TaskDescription]. Reads [TaskDescriptionHint]s
     *
     * @param out [DataInput2] to read from.
     * @param mediaItems [DAO] to lookup [MediaItem]s
     *
     * @return Deserialized [TaskDescriptionTarget]s.
     */
    private fun readTaskDescriptionComponents(input: DataInput2, available: Int, mediaItems: DAO<MediaItem>) : List<TaskDescriptionHint> = (0 until input.unpackInt()).map {
        val start = input.unpackLong().let { if (it == -1L) null else it  }
        val end = input.unpackLong().let { if (it == -1L) null else it  }
        when(val ordinal = input.unpackInt()) {
            1 -> TaskDescriptionHint.TextTaskDescriptionHint(input.readUTF(), start, end)
            2 -> TaskDescriptionHint.ImageItemTaskDescriptionHint(mediaItems[input.readUID()]!! as MediaItem.ImageItem, start, end)
            3 -> TaskDescriptionHint.VideoItemSegmentTaskDescriptionHint(mediaItems[input.readUID()]!! as MediaItem.VideoItem, TemporalRangeSerializer.deserialize(input, available), start, end)
            4 -> TaskDescriptionHint.ExternalImageTaskDescriptionHint(Paths.get(input.readUTF()), start, end)
            5 -> TaskDescriptionHint.ExternalVideoTaskDescriptionHint(Paths.get(input.readUTF()), start, end)
            else -> throw IllegalArgumentException("Failed to deserialize Task Description Hint for ordinal $ordinal; not implemented.")
        }
    }
}
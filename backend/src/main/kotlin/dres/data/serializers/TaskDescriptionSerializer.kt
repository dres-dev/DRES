package dres.data.serializers

import dres.data.dbo.DAO
import dres.data.model.basics.media.MediaItem
import dres.data.model.competition.*
import dres.data.model.competition.interfaces.TaskDescription
import dres.utilities.extensions.readUID
import dres.utilities.extensions.writeUID
import org.mapdb.DataInput2
import org.mapdb.DataOutput2

object TaskDescriptionSerializer {
    fun serialize(out: DataOutput2, value: TaskDescription) { //FIXME

        out.writeUID(value.id)
        out.writeUTF(value.name)
        out.writeUTF(value.taskGroup.name)
        out.writeUTF(value.taskType.name)
        out.packLong(value.duration)
        out.writeUID(value.defaultMediaCollectionId)
        writeTaskDescriptionComponents(out, value.components)
        writeTaskDescriptionTarget(out, value.target)

    }

    private fun writeTaskDescriptionComponents(out: DataOutput2, components: List<TaskDescriptionComponent>){

        out.packInt(components.size)
        components.forEach {
            out.packLong(it.start ?: -1)
            out.packLong(it.end ?: -1)
            out.writeUTF(it.javaClass.name)
            when(it) {
                is TextTaskDescriptionComponent -> {
                    out.writeUTF(it.text)
                }
                is VideoItemSegmentTaskDescriptionComponent -> {
                    out.writeUID(it.item.id)
                    TemporalRangeSerializer.serialize(out, it.temporalRange)
                }
                is ImageItemTaskDescriptionComponent -> {
                    out.writeUID(it.item.id)
                }
                is ExternalImageTaskDescriptionComponent -> {
                    out.writeUTF(it.imageLocation)
                }
                is ExternalVideoTaskDescriptionComponent -> {
                    out.writeUTF(it.videoLocation)
                }
                else -> throw IllegalArgumentException("serialization of ${it.javaClass.simpleName} not implemented")
            }
        }

    }

    private fun readTaskDescriptionComponents(input: DataInput2, mediaItems: DAO<MediaItem>) : List<TaskDescriptionComponent> = (0 until input.unpackInt()).map {
        val start = input.unpackLong().let { if (it == -1L) null else it  }
        val end = input.unpackLong().let { if (it == -1L) null else it  }
        when(input.readUTF()) {
            TextTaskDescriptionComponent::javaClass.name -> {
                TextTaskDescriptionComponent(input.readUTF(), start, end)
            }
            VideoItemSegmentTaskDescriptionComponent::javaClass.name-> {
                VideoItemSegmentTaskDescriptionComponent(mediaItems[input.readUID()]!! as MediaItem.VideoItem, TemporalRangeSerializer.deserialize(input, 0), start, end)
            }
            ImageItemTaskDescriptionComponent::javaClass.name -> {
                ImageItemTaskDescriptionComponent(mediaItems[input.readUID()]!! as MediaItem.ImageItem, start, end)
            }
            ExternalImageTaskDescriptionComponent::javaClass.name -> {
                ExternalImageTaskDescriptionComponent(input.readUTF(), start, end)
            }
            ExternalVideoTaskDescriptionComponent::javaClass.name -> {
                ExternalVideoTaskDescriptionComponent(input.readUTF(), start, end)
            }
            else -> throw IllegalArgumentException("deserialization of ${it.javaClass.simpleName} not implemented")
        }
    }

    private fun writeTaskDescriptionTarget(out: DataOutput2, target: TaskDescriptionTarget) {

        out.writeUTF(out.javaClass.name)
        when(target) {
            is JudgementTaskDescriptionTarget -> {}
            is MediaSegmentTarget -> TODO()
            else -> throw IllegalArgumentException("deserialization of ${target.javaClass.simpleName} not implemented")
        }
    }

    private fun readTaskDescriptionTarget(input: DataInput2) : TaskDescriptionTarget {
        val className = input.readUTF()
         return when(className) {
             JudgementTaskDescriptionTarget::javaClass.name -> JudgementTaskDescriptionTarget
             MediaSegmentTarget::javaClass.name -> TODO()
             else -> throw IllegalArgumentException("deserialization of ${className.javaClass.simpleName} not implemented")
         }
    }

    fun deserialize(input: DataInput2, taskGroups: List<TaskGroup>, taskTypes: List<TaskType>, mediaItems: DAO<MediaItem>): TaskDescription {

        val uid = input.readUID()
        val name = input.readUTF()
        val taskGroupName = input.readUTF()
        val taskTypeName = input.readUTF()
        val duration = input.unpackLong()
        val defaultMediaCollectionId = input.readUID()
        val components = readTaskDescriptionComponents(input, mediaItems)
        val target = readTaskDescriptionTarget(input)

        return TaskDescription(
                uid,
                name,
                taskGroups.find { it.name == taskGroupName }!!,
                taskTypes.find { it.name == taskTypeName }!!,
                duration,
                defaultMediaCollectionId,
                components,
                target
        )

    }
}
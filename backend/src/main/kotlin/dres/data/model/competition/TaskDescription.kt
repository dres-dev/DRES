package dres.data.model.competition

import dres.data.model.basics.MediaItem
import dres.data.model.basics.TemporalRange
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonInput
import kotlinx.serialization.json.JsonObject

/**
 * General [TaskDescription]
 *
 * @author Ralph Gasser
 * @version 1.0
 */
@Serializable
sealed class TaskDescription(val taskType: TaskType) {


//    @Serializer(forClass = TaskDescription::class)
//    companion object : KSerializer<TaskDescription> {
//
//        override fun serialize(encoder: Encoder, obj: TaskDescription) {
//            when (obj) {
//                is KisVisualTaskDescription -> encoder.encode(KisVisualTaskDescription.serializer(), obj)
//                is KisTextualTaskDescription -> encoder.encode(KisTextualTaskDescription.serializer(), obj)
//                is AvsTaskDescription -> encoder.encode(AvsTaskDescription.serializer(), obj)
//            }
//        }
//
//        override fun deserialize(decoder: Decoder): TaskDescription{
//
//            val input = decoder as? JsonInput
//                    ?: throw SerializationException("Expected JsonInput for ${decoder::class}")
//            val jsonObject = input.decodeJson() as? JsonObject
//                    ?: throw SerializationException("Expected JsonObject for ${input.decodeJson()::class}")
//
//            return when (TaskType.valueOf(jsonObject.getPrimitive("taskType").content)) {
//                TaskType.AVS -> decoder.json.parse(AvsTaskDescription.serializer(), jsonObject.toString())
//                TaskType.KIS_VISUAL -> decoder.json.parse(KisVisualTaskDescription.serializer(), jsonObject.toString())
//                TaskType.KIS_TEXTUAL -> {
//
//
//                    return KisTextualTaskDescription(
//                            decoder.json.parse(MediaItem.serializer(), jsonObject.getObject("item").toString()) as MediaItem.VideoItem,
//                            decoder.json.parse(TemporalRange.serializer(), jsonObject.getObject("temporalRange").toString()),
//                            decoder.json.parse(List<String>.serializer(), jsonObject.getObject("temporalRange").toString()),
//                    )
//                }
//            }
//
//        }
//
//    }


    /**
     * Describes a  [TaskType.KIS_VISUAL] [Task]
     *
     * @param item [MediaItem] the user should be looking for.
     */
    @Polymorphic
    @Serializable
    data class KisVisualTaskDescription(val item: MediaItem.VideoItem, val temporalRange: TemporalRange) : TaskDescription(TaskType.KIS_VISUAL)

    /**
     * Describes a [TaskType.KIS_TEXTUAL] [Task]
     *
     * @param item [MediaItem] the user should be looking for.
     */
    @Polymorphic
    @Serializable
    data class KisTextualTaskDescription(val item: MediaItem.VideoItem, val temporalRange: TemporalRange, val descriptions: List<String>, val delay: Int = 30) : TaskDescription(TaskType.KIS_TEXTUAL)

    /**
     * Describes a [TaskType.AVS] video [Task]
     *
     * @param description Textual task description presented to the user.
     */
    @Polymorphic
    @Serializable
    data class AvsTaskDescription(val description: String) : TaskDescription(TaskType.AVS)
}


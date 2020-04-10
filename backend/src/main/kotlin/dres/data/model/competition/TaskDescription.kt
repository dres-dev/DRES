package dres.data.model.competition

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import dres.data.model.basics.MediaItem
import dres.data.model.basics.TemporalRange
import dres.data.model.competition.interfaces.TaskDescription
import dres.data.model.competition.interfaces.MediaSegmentTaskDescription
import dres.run.validate.JudgementValidator
import dres.run.validate.SubmissionValidator
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable


/**
 * General [TaskDescription]
 *
 * @author Ralph Gasser
 * @version 1.0
 */
@Serializable
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "taskType")
@JsonSubTypes(
    JsonSubTypes.Type(value = TaskDescriptionBase.KisVisualTaskDescription::class, name = "KIS_VISUAL"),
    JsonSubTypes.Type(value = TaskDescriptionBase.KisTextualTaskDescription::class, name = "KIS_TEXTUAL"),
    JsonSubTypes.Type(value = TaskDescriptionBase.AvsTaskDescription::class, name = "AVS")
)
sealed class TaskDescriptionBase(override val taskType: TaskType): TaskDescription {

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
    data class KisVisualTaskDescription(override val item: MediaItem.VideoItem, override val temporalRange: TemporalRange) : TaskDescriptionBase(TaskType.KIS_VISUAL), MediaSegmentTaskDescription {

    }

    /**
     * Describes a [TaskType.KIS_TEXTUAL] [Task]
     *
     * @param item [MediaItem] the user should be looking for.
     */
    @Polymorphic
    @Serializable
    data class KisTextualTaskDescription(override val item: MediaItem.VideoItem, override val temporalRange: TemporalRange, val descriptions: List<String>, val delay: Int = 30) : TaskDescriptionBase(TaskType.KIS_TEXTUAL), MediaSegmentTaskDescription

    /**
     * Describes a [TaskType.AVS] video [Task]
     *
     * @param description Textual task description presented to the user.
     */
    @Polymorphic
    @Serializable
    data class AvsTaskDescription(val description: String) : TaskDescriptionBase(TaskType.AVS)
}



package dres.data.model.competition

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import dres.data.model.basics.media.MediaItem
import dres.data.model.basics.time.TemporalRange
import dres.data.model.competition.interfaces.DefinedMediaItemTaskDescription
import dres.data.model.competition.interfaces.HiddenResultsTaskDescription
import dres.data.model.competition.interfaces.MediaSegmentTaskDescription
import dres.data.model.competition.interfaces.TaskDescription
import dres.data.model.run.Submission
import dres.run.filter.DuplicateSubmissionFilter
import dres.run.filter.OneCorrectSubmissionPerTeamFilter
import dres.run.filter.SubmissionFilter
import dres.run.score.interfaces.TaskRunScorer
import dres.run.score.scorer.AvsTaskScorer
import dres.run.score.scorer.KisTaskScorer
import dres.run.validation.TemporalOverlapSubmissionValidator
import dres.run.validation.judged.BasicJudgementValidator
import java.util.*

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "taskType")
@JsonSubTypes(
        JsonSubTypes.Type(value = TaskDescriptionBase.KisVisualTaskDescription::class, name = "KIS_VISUAL"),
        JsonSubTypes.Type(value = TaskDescriptionBase.KisTextualTaskDescription::class, name = "KIS_TEXTUAL"),
        JsonSubTypes.Type(value = TaskDescriptionBase.AvsTaskDescription::class, name = "AVS")
)
sealed class TaskDescriptionBase : TaskDescription {


    /** Helper property for de/serialization. */
    val taskType: String
        get() = this.taskGroup.type.name

    /**
     * Describes a visual Known Item Search (KIS)
     *
     * @param item [MediaItem] the user should be looking for.
     */
    data class KisVisualTaskDescription @JsonCreator constructor(
            @JsonProperty("uid") override val uid: String = UUID.randomUUID().toString(),
            @JsonProperty("name") override val name: String,
            @JsonProperty("taskGroup") override val taskGroup: TaskGroup,
            @JsonProperty("duration") override val duration: Long,
            @JsonProperty("item") override val item: MediaItem.VideoItem,
            @JsonProperty("temporalRange") override val temporalRange: TemporalRange)
        : TaskDescriptionBase(), MediaSegmentTaskDescription {
        override fun newScorer(): TaskRunScorer = KisTaskScorer()
        override fun newValidator(callback: ((Submission) -> Unit)?) = TemporalOverlapSubmissionValidator(this, callback)
        override fun cacheItemName() = "${taskGroup.name}-${item.collection}-${item.id}-${temporalRange.start.value}-${temporalRange.end.value}.mp4"
        override fun newFilter(): SubmissionFilter = OneCorrectSubmissionPerTeamFilter() and DuplicateSubmissionFilter()
    }

    /**
     * Describes a textual Known Item Search (KIS) [Task]
     *
     * @param item [MediaItem] the user should be looking for.
     */
    data class KisTextualTaskDescription @JsonCreator constructor(
            @JsonProperty("uid") override val uid: String = UUID.randomUUID().toString(),
            @JsonProperty("name") override val name: String,
            @JsonProperty("taskGroup") override val taskGroup: TaskGroup,
            @JsonProperty("duration") override val duration: Long,
            @JsonProperty("item") override val item: MediaItem.VideoItem,
            @JsonProperty("temporalRange") override val temporalRange: TemporalRange,
            @JsonProperty("descriptions") val descriptions: List<String>,
            @JsonProperty("delay") val delay: Int = 30)
        : TaskDescriptionBase(), MediaSegmentTaskDescription, HiddenResultsTaskDescription {
        override fun newScorer(): TaskRunScorer = KisTaskScorer()
        override fun newValidator(callback: ((Submission) -> Unit)?) = TemporalOverlapSubmissionValidator(this, callback)
        override fun cacheItemName() = "${taskGroup.name}-${item.collection}-${item.id}-${temporalRange.start.value}-${temporalRange.end.value}.mp4"
        override fun newFilter(): SubmissionFilter = OneCorrectSubmissionPerTeamFilter() and DuplicateSubmissionFilter()
    }

    /**
     * Describes a AVS type video [Task]
     *
     * @param description Textual task description presented to the user.
     */
    data class AvsTaskDescription @JsonCreator constructor(
            @JsonProperty("uid") override val uid: String = UUID.randomUUID().toString(),
            @JsonProperty("name") override val name: String,
            @JsonProperty("taskGroup") override val taskGroup: TaskGroup,
            @JsonProperty("duration") override val duration: Long,
            @JsonProperty("description") val description: String,
            @JsonProperty("defaultCollection") val defaultCollection: Long)
        : TaskDescriptionBase(), TaskDescription, DefinedMediaItemTaskDescription {
        override fun newScorer(): TaskRunScorer = AvsTaskScorer()
        override fun newValidator(callback: ((Submission) -> Unit)?) = BasicJudgementValidator(callback)
        override fun newFilter(): SubmissionFilter = DuplicateSubmissionFilter()
    }
}

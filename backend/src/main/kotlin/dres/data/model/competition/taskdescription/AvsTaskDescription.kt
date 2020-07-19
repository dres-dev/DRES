package dres.data.model.competition.taskdescription

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import dres.data.model.Config
import dres.data.model.competition.QueryContent
import dres.data.model.competition.QueryContentElement
import dres.data.model.competition.QueryDescription
import dres.data.model.competition.TaskGroup
import dres.data.model.competition.interfaces.DefinedMediaItemTaskDescription
import dres.data.model.competition.interfaces.TaskDescription
import dres.run.filter.DuplicateSubmissionFilter
import dres.run.filter.SubmissionFilter
import dres.run.filter.TemporalSubmissionFilter
import dres.run.score.interfaces.TaskRunScorer
import dres.run.score.scorer.AvsTaskScorer
import dres.run.validation.judged.BasicJudgementValidator
import java.util.*

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
    : TaskDescription, DefinedMediaItemTaskDescription {


    override fun toQueryDescription(config: Config): QueryDescription = QueryDescription(name, QueryContent(text = listOf(QueryContentElement(description, "text/plain"))))

    override fun newScorer(): TaskRunScorer = AvsTaskScorer()
    override fun newValidator() = BasicJudgementValidator()
    override fun newFilter(): SubmissionFilter = TemporalSubmissionFilter() and DuplicateSubmissionFilter()
}
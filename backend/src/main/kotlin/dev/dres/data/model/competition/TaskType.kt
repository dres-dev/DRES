package dev.dres.data.model.competition

import dev.dres.run.filter.*
import dev.dres.run.score.interfaces.TaskRunScorer
import dev.dres.run.score.scorer.AvsTaskScorer
import dev.dres.run.score.scorer.KisTaskScorer
import dev.dres.run.validation.interfaces.SubmissionValidator

interface Option {
    val ordinal: Int
}

data class ConfiguredOption<T: Option>(val option: T, val parameters : Map<String, String> = emptyMap())

data class TaskType(
        val name: String,
        val taskDuration: Long, //in ms
        val targetType: ConfiguredOption<TargetType>,
        val components: Collection<ConfiguredOption<QueryComponentType>>,
        val score: ConfiguredOption<ScoringType>,
        val filter: Collection<ConfiguredOption<SubmissionFilterType>>,
        val options: Collection<ConfiguredOption<Options>>
) {

    enum class Options : Option{
        HIDDEN_RESULTS, //Do not show submissions while task is running
        MAP_TO_SEGMENT //Map the time of a submission to a pre-defined segment
    }

    enum class TargetType : Option{
        SINGLE_MEDIA_ITEM, // Whole Media Item"
        SINGLE_MEDIA_SEGMENT, //Part of a Media Item
        MULTIPLE_MEDIA_ITEMS, //Multiple Media Items
        JUDGEMENT, //Judgement
        VOTE //Judgement with audience voting
    }

    enum class QueryComponentType : Option{
        IMAGE_ITEM, //Image Media Item
        VIDEO_ITEM_SEGMENT, //Part of a Video Media Item
        TEXT,
        EXTERNAL_IMAGE,
        EXTERNAL_VIDEO
    }

    enum class ScoringType : Option{
        KIS,
        AVS
    }

    enum class SubmissionFilterType(internal val filter: (parameters: Map<String, String>) -> SubmissionFilter) : Option{
        NO_DUPLICATES({_ -> DuplicateSubmissionFilter()}),
        LIMIT_CORRECT_PER_TEAM({params -> CorrectSubmissionPerTeamFilter(params)}),
        LIMIT_WRONG_PER_TEAM({params ->  MaximumWrongSubmissionsPerTeam(params)}),
        LIMIT_TOTAL_PER_TEAM({params ->  MaximumTotalSubmissionsPerTeam(params)}),
        LIMIT_CORRECT_PER_MEMBER({params -> CorrectSubmissionPerTeamMemberFilter(params)}),
        TEMPORAL_SUBMISSION({_ ->TemporalSubmissionFilter()})
    }

    /**
     * Generates a new [TaskRunScorer] for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [TaskRunScorer].
     */
    fun newScorer(): TaskRunScorer = when(score.option){
        ScoringType.KIS -> KisTaskScorer(score.parameters)
        ScoringType.AVS -> AvsTaskScorer()
    }

    /**
     * Generates and returns a [SubmissionValidator] instance for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [SubmissionFilter]
     */
    fun newFilter(): SubmissionFilter {
        if (filter.isEmpty()){
            return AllSubmissionFilter
        }

        return filter.map { it.option.filter(it.parameters) }.reduceRight(SubmissionFilter::and)
    }

}
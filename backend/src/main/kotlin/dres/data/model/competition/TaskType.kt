package dres.data.model.competition

import dres.run.filter.*
import dres.run.score.interfaces.TaskRunScorer
import dres.run.score.scorer.AvsTaskScorer
import dres.run.score.scorer.KisTaskScorer
import dres.run.validation.interfaces.SubmissionValidator

data class TaskType(
        val name: String,
        val taskDuration: Long, //in ms
        val targetType: TargetType,
        val components: Set<QueryComponentType>,
        val score: ScoringType,
        val filter: Set<SubmissionFilterType>,
        val options: Set<Options>
) {

    enum class Options{

        HIDDEN_RESULTS, //Do not show submissions while task is running
        MAP_TO_SEGMENT //Map the time of a submission to a pre-defined segment

    }

    enum class TargetType{

        SINGLE_MEDIA_ITEM, // Whole Media Item"
        SINGLE_MEDIA_SEGMENT, //Part of a Media Item
        MULTIPLE_MEDIA_ITEMS, //Multiple Media Items
        JUDGEMENT //Judgement

    }

    enum class QueryComponentType{

        IMAGE_ITEM, //Image Media Item
        VIDEO_ITEM_SEGMENT, //Part of a Video Media Item
        TEXT,
        EXTERNAL_IMAGE,
        EXTERNAL_VIDEO

    }

    enum class ScoringType{

        KIS,
        AVS

    }

    enum class SubmissionFilterType(internal val filter: () -> SubmissionFilter){

        NO_DUPLICATES( ::DuplicateSubmissionFilter),
        ONE_CORRECT_PER_TEAM(::OneCorrectSubmissionPerTeamFilter),
        TEMPORAL_SUBMISSION(::TemporalSubmissionFilter)

    }


    /**
     * Generates a new [TaskRunScorer] for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [TaskRunScorer].
     */
    fun newScorer(): TaskRunScorer = when(score){
        ScoringType.KIS -> KisTaskScorer()
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

        return filter.map { it.filter() }.reduceRight(SubmissionFilter::and)


    }

}
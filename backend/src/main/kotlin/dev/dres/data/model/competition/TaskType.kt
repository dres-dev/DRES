package dev.dres.data.model.competition

import dev.dres.data.model.competition.options.*
import dev.dres.run.filter.AllSubmissionFilter
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.filter.SubmissionFilterAggregator
import dev.dres.run.score.interfaces.TaskScorer
import dev.dres.run.validation.interfaces.SubmissionValidator


data class TaskType(
    val name: String,
    val taskDuration: Long, //in ms
    val targetType: ConfiguredOption<TargetOption>,
    val components: Collection<ConfiguredOption<QueryComponentOption>>,
    val score: ConfiguredOption<ScoringOption>,
    val filter: Collection<ConfiguredOption<SubmissionFilterOption>>,
    val options: Collection<ConfiguredOption<SimpleOption>>
) {

    /**
     * Generates a new [TaskScorer] for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [TaskScorer].
     */
    fun newScorer(): TaskScorer = this.score.option.scorer(this.score.parameters)

    /**
     * Generates and returns a [SubmissionValidator] instance for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [SubmissionFilter]
     */
    fun newFilter(): SubmissionFilter {
        if (this.filter.isEmpty()) {
            return AllSubmissionFilter
        }
        return SubmissionFilterAggregator(this.filter.map { it.option.filter(it.parameters) })
    }
}
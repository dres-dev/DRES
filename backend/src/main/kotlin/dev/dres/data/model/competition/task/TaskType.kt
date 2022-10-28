package dev.dres.data.model.competition.task

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.task.options.*
import dev.dres.data.model.competition.task.options.ConfiguredOption
import dev.dres.run.filter.AllSubmissionFilter
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.filter.SubmissionFilterAggregator
import dev.dres.run.score.interfaces.TaskScorer
import dev.dres.run.validation.interfaces.SubmissionValidator
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.isEmpty
import kotlinx.dnq.query.query
import kotlinx.dnq.simple.min

/**
 * Specifies the type of a [TaskDescription] and allows for many aspects of its configuration.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
class TaskType(entity: Entity) : XdEntity(entity) {
    /** Combination of [TaskType] name / competition must be unique. */
    companion object: XdNaturalEntityType<TaskType>() {
        override val compositeIndices = listOf(
            listOf(TaskType::name, TaskType::competition)
        )
    }

    /** The name of this [TaskType]. */
    var name by xdRequiredStringProp(unique = false, trimmed = false)

    /** The [CompetitionDescription] this [TaskType] belongs to. */
    var competition by xdParent<TaskType,CompetitionDescription>(CompetitionDescription::taskTypes)

    /** The (default) duration of this [TaskType] in seconds. */
    var duration by xdRequiredLongProp() { min(0L) }

    /** The [TaskTargetOption] for this [TaskType]. Specifies the type of target. */
    var target by xdLink1(TaskTargetOption)

    /** The [TaskScoreOption] for this [TaskType]. Specifies the type of scorer that should be used. */
    var score by xdLink1(TaskScoreOption)

    /** The [TaskComponentOption]s that make-up this [TaskType]. */
    val components by xdLink0_N(TaskComponentOption)

    /** The [TaskSubmissionOption]s for this [TaskType]. */
    val submission by xdLink0_N(TaskSubmissionOption)

    /** The [TaskOption]s for this [TaskType]. */
    val options by xdLink0_N(TaskOption)

    /** [ConfiguredOption]s registered for this [TaskDescription]. */
    val configurations by xdChildren0_N<TaskType,ConfiguredOption>(ConfiguredOption::task)

    /**
     * Generates a new [TaskScorer] for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * Calling this method requires an ongoing transaction!
     *
     * @return [TaskScorer].
     */
    fun newScorer(): TaskScorer {
        val parameters = this.configurations.query(ConfiguredOption::key eq this.score.description)
            .asSequence().map { it.key to it.value }.toMap()
        return this.score.scorer(parameters)
    }

    /**
     * Generates and returns a [SubmissionValidator] instance for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * Calling this method requires an ongoing transaction!
     *
     * @return [SubmissionFilter]
     */
    fun newFilter(): SubmissionFilter {
        if (this.submission.isEmpty) return AllSubmissionFilter
        val filters = this.submission.asSequence().map { option ->
            val parameters = this.configurations.query(ConfiguredOption::key eq this.score.description)
                .asSequence().map { it.key to it.value }.toMap()
            option.newFilter(parameters)
        }.toList()
        return SubmissionFilterAggregator(filters)
    }
}
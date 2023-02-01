package dev.dres.data.model.template.task

import dev.dres.api.rest.types.competition.tasks.ApiTaskType
import dev.dres.data.model.template.EvaluationTemplate
import dev.dres.data.model.template.task.options.*
import dev.dres.data.model.template.task.options.ConfiguredOption
import dev.dres.run.filter.AllSubmissionFilter
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.filter.SubmissionFilterAggregator
import dev.dres.run.score.interfaces.TaskScorer
import dev.dres.run.validation.interfaces.SubmissionValidator
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.*
import kotlinx.dnq.simple.min

/**
 * Specifies the type of a [TaskTemplate] and allows for many aspects of its configuration.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
class TaskType(entity: Entity) : XdEntity(entity) {
    /** Combination of [TaskType] name / competition must be unique. */
    companion object: XdNaturalEntityType<TaskType>() {
        override val compositeIndices = listOf(
            listOf(TaskType::name, TaskType::evaluation)
        )
    }

    /** The name of this [TaskType]. */
    var name by xdRequiredStringProp(unique = false, trimmed = false)

    /** The [EvaluationTemplate] this [TaskType] belongs to. */
    var evaluation: EvaluationTemplate by xdParent<TaskType,EvaluationTemplate>(EvaluationTemplate::taskTypes)

    /** The (default) duration of this [TaskType] in seconds. */
    var duration by xdRequiredLongProp() { min(0L) }

    /** The [TargetOption] for this [TaskType]. Specifies the type of target. */
    var target by xdLink1(TargetOption)

    /** The [HintOption]s that make-up this [TaskType]. */
    val hints by xdLink0_N(HintOption)

    /** The [SubmissionOption]s for this [TaskType]. */
    val submission by xdLink0_N(SubmissionOption)

    /** The [ScoreOption] for this [TaskType]. Specifies the type of scorer that should be used. */
    var score by xdLink1(ScoreOption)

    /** The [TaskOption]s for this [TaskType]. */
    val options by xdLink0_N(TaskOption)

    /** [ConfiguredOption]s registered for this [TaskTemplate]. */
    val configurations by xdChildren0_N<TaskType,ConfiguredOption>(ConfiguredOption::task)

    /**
     * Converts this [TaskType] to a RESTful API representation [ApiTaskType].
     *
     * @return [ApiTaskType]
     */
    fun toApi(): ApiTaskType = ApiTaskType(
        name = this.name,
        duration = this.duration,
        targetOption = this.target.toApi(),
        hintOptions = this.hints.asSequence().map { it.toApi() }.toList(),
        submissionOptions = this.submission.asSequence().map { it.toApi() }.toList(),
        taskOptions = this.options.asSequence().map { it.toApi() }.toList(),
        scoreOption = this.score.toApi(),
        configuration = this.configurations.asSequence().map { it.key to it.value }.toMap()
    )

    /**
     * Generates a new [TaskScorer] for this [TaskTemplate]. Depending
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
     * Generates and returns a [SubmissionValidator] instance for this [TaskTemplate]. Depending
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
package dev.dres.data.model.run

import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.run.interfaces.EvaluationRun
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.data.model.template.task.options.DbTaskOption
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.score.scoreboard.MaxNormalizingScoreBoard
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.score.scoreboard.SumAggregateScoreBoard
import dev.dres.run.score.scorer.CachingTaskScorer
import dev.dres.run.score.scorer.TaskScorer
import dev.dres.run.transformer.MapToSegmentTransformer
import dev.dres.run.transformer.SubmissionTaskMatchFilter
import dev.dres.run.transformer.SubmissionTransformer
import dev.dres.run.transformer.SubmissionTransformerAggregator
import kotlinx.dnq.query.*


/**
 * Represents a concrete, interactive and synchronous [Run] of a [DbEvaluationTemplate].
 *
 * [InteractiveSynchronousEvaluation]s can be started, ended and they can be used to create new [TaskRun]s and access the current [TaskRun].
 *
 * @author Luca Rossetto
 * @param 1.0.0
 */
class NonInteractiveEvaluation(evaluation: DbEvaluation) : AbstractEvaluation(evaluation) {

    init {
        require(this.evaluation.type == DbEvaluationType.NON_INTERACTIVE) { "Incompatible competition type ${this.evaluation.type}. This is a programmer's error!" }
        require(this.description.tasks.size() > 0) { "Cannot create a run from a competition that doesn't have any tasks." }
        require(this.description.teams.size() > 0) { "Cannot create a run from a competition that doesn't have any teams." }
    }

    /** List of [TaskRun]s registered for this [NonInteractiveEvaluation]. */
    override val tasks = this.evaluation.tasks.asSequence().map {
        NITaskRun(it)
    }.toList()

    /** List of [Scoreboard]s maintained by this [NonInteractiveEvaluation]. */
    override val scoreboards: List<Scoreboard>

    init {
        val teams = this.description.teams.asSequence().map { it.teamId }.toList()
        val groupBoards = this.description.taskGroups.asSequence().map { group ->
            MaxNormalizingScoreBoard(group.name, this, teams, {task -> task.taskGroup.name == group.name}, group.name)
        }.toList()
        val aggregateScoreBoard = SumAggregateScoreBoard("sum", this, groupBoards)
        this.scoreboards = groupBoards.plus(aggregateScoreBoard)
    }

    /**
     * The [TaskRun] used by a [NonInteractiveEvaluation].
     */
    inner class NITaskRun(task: DbTask): AbstractNonInteractiveTask(task) {
        /** Reference to the [EvaluationRun] hosting this [NITaskRun]. */
        override val competition: EvaluationRun
            get() = this@NonInteractiveEvaluation

        /** The position of this [NITaskRun] within the [NonInteractiveEvaluation]. */
        override val position: Int
            get() = this@NonInteractiveEvaluation.tasks.indexOf(this)

        /** The [CachingTaskScorer] instance used by this [NITaskRun]. */
        override val scorer: CachingTaskScorer = TODO("Will we have the same scorers for non-interactive tasks.")

        override val transformer: SubmissionTransformer = if (this.template.taskGroup.type.options.filter { it eq DbTaskOption.MAP_TO_SEGMENT }.any()) {
            SubmissionTransformerAggregator(
                listOf(
                    SubmissionTaskMatchFilter(this.taskId),
                    MapToSegmentTransformer()
                )
            )
        } else {
            SubmissionTaskMatchFilter(this.taskId)
        }

        /** The [SubmissionFilter] instance used by this [NITaskRun]. */
        override val filter: SubmissionFilter = TODO("Can there be submission filters for non-interactive tasks?")

        /** List of [TeamId]s that work on this [NITaskRun]. */
        override val teams: List<TeamId> = this@NonInteractiveEvaluation.description.teams.asSequence().map { it.teamId }.toList()

        /** */
        override val duration: Long = 0
    }
}
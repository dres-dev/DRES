package dev.dres.data.model.run

import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.run.interfaces.EvaluationRun
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.data.model.template.task.options.DbConfiguredOption
import dev.dres.data.model.template.task.options.DbScoreOption
import dev.dres.data.model.template.task.options.DbTaskOption
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.filter.basics.AcceptAllSubmissionFilter
import dev.dres.run.filter.basics.SubmissionFilter
import dev.dres.run.filter.basics.CombiningSubmissionFilter
import dev.dres.run.score.scoreboard.MaxNormalizingScoreBoard
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.score.scorer.*
import dev.dres.run.score.scorer.AvsTaskScorer
import dev.dres.run.score.scorer.CachingTaskScorer
import dev.dres.run.transformer.MapToSegmentTransformer
import dev.dres.run.transformer.SubmissionTaskMatchTransformer
import dev.dres.run.transformer.basics.SubmissionTransformer
import dev.dres.run.transformer.basics.CombiningSubmissionTransformer
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
        this.scoreboards = this.description.taskGroups.asSequence().map { group ->
            MaxNormalizingScoreBoard(group.name, this, teams, {task -> task.taskGroup.name == group.name}, group.name)
        }.toList()
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
        override val scorer: CachingTaskScorer = CachingTaskScorer(
            when(val scoreOption = this.template.taskGroup.type.score) {
                DbScoreOption.KIS -> throw IllegalStateException("KIS task scorer is not applicable to non-interactive evaluations")
                DbScoreOption.AVS -> AvsTaskScorer(this)
                DbScoreOption.LEGACY_AVS -> LegacyAvsTaskScorer(this)
                else -> throw IllegalStateException("The task score option $scoreOption is currently not supported.")
            }
        )

        override val transformer: SubmissionTransformer = if (this.template.taskGroup.type.options.filter { it eq DbTaskOption.MAP_TO_SEGMENT }.any()) {
            CombiningSubmissionTransformer(
                listOf(
                    SubmissionTaskMatchTransformer(this.taskId),
                    MapToSegmentTransformer()
                )
            )
        } else {
            SubmissionTaskMatchTransformer(this.taskId)
        }

        /** The [SubmissionFilter] instance used by this [NITaskRun]. */
        override val filter: SubmissionFilter

        init{
            if (this.template.taskGroup.type.submission.isEmpty) {
                this.filter = AcceptAllSubmissionFilter
            } else {
                this.filter = CombiningSubmissionFilter(
                    this.template.taskGroup.type.submission.asSequence().map { option ->
                        val parameters = this.template.taskGroup.type.configurations.query(DbConfiguredOption::key eq option.description)
                            .asSequence().map { it.key to it.value }.toMap()
                        option.newFilter(parameters)
                    }.toList()
                )
            }
        }

        /** List of [TeamId]s that work on this [NITaskRun]. */
        override val teams: List<TeamId> = this@NonInteractiveEvaluation.description.teams.asSequence().map { it.teamId }.toList()

        /** */
        override val duration: Long = 0
    }
}

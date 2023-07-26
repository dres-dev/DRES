package dev.dres.data.model.run

import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.template.TemplateId
import dev.dres.data.model.template.task.options.DbConfiguredOption
import dev.dres.data.model.template.task.options.DbScoreOption
import dev.dres.data.model.template.task.options.DbTaskOption
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.filter.AllSubmissionFilter
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.filter.SubmissionFilterAggregator
import dev.dres.run.score.scoreboard.MaxNormalizingScoreBoard
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.score.scoreboard.SumAggregateScoreBoard
import dev.dres.run.score.scorer.AvsTaskScorer
import dev.dres.run.score.scorer.CachingTaskScorer
import dev.dres.run.score.scorer.KisTaskScorer
import dev.dres.run.score.scorer.LegacyAvsTaskScorer
import dev.dres.run.transformer.MapToSegmentTransformer
import dev.dres.run.transformer.SubmissionTaskMatchFilter
import dev.dres.run.transformer.SubmissionTransformer
import dev.dres.run.transformer.SubmissionTransformerAggregator
import kotlinx.dnq.query.*
import java.lang.IndexOutOfBoundsException
import java.util.LinkedList

/**
 * Represents a concrete, interactive and synchronous [Run] of a [DbEvaluationTemplate].
 *
 * [InteractiveSynchronousEvaluation]s can be started, ended, and they can be used to create new [TaskRun]s and access the current [TaskRun].
 *
 * @author Ralph Gasser
 * @param 2.0.0
 */
class InteractiveSynchronousEvaluation(evaluation: DbEvaluation) : AbstractEvaluation(evaluation) {

    init {
        require(this.evaluation.type == DbEvaluationType.INTERACTIVE_SYNCHRONOUS) { "Incompatible competition type ${this.evaluation.type}. This is a programmer's error!" }
        require(this.description.tasks.size() > 0) { "Cannot create a run from a competition that doesn't have any tasks." }
        require(this.description.teams.size() > 0) { "Cannot create a run from a competition that doesn't have any teams." }
    }

    /** List of [TaskRun]s registered for this [InteractiveSynchronousEvaluation]. */
    override val tasks = LinkedList<ISTaskRun>()

    /** Reference to the currently active [DbTaskTemplate]. This is part of the task navigation. */
    private val templates = this.description.tasks.sortedBy(DbTaskTemplate::idx).asSequence().map { it.templateId }.toList()

    /** Returns the last [TaskRun]. */
    val currentTask: AbstractInteractiveTask?
        get() = this.tasks.lastOrNull { it.templateId == this.templates[this.templateIndex] }

    /** The index of the task template this [InteractiveSynchronousEvaluation] is pointing to. */
    var templateIndex: Int = this.currentTask?.templateId?.let { this.templates.indexOf(it) } ?: 0
        private set

    /** List of [Scoreboard]s maintained by this [NonInteractiveEvaluation]. */
    override val scoreboards: List<Scoreboard>

    init {
        /* Load all ongoing tasks. */
        this.evaluation.tasks.asSequence().forEach { ISTaskRun(it) }

        /* Prepare the evaluation scoreboards. */
        val teams = this.description.teams.asSequence().map { it.teamId }.toList()
        val groupBoards = this.description.taskGroups.asSequence().map { group ->
            MaxNormalizingScoreBoard(group.name, this, teams, {task -> task.taskGroup.name == group.name}, group.name)
        }.toList()
        val aggregateScoreBoard = SumAggregateScoreBoard("sum", this, groupBoards)
        this.scoreboards = groupBoards.plus(aggregateScoreBoard)
    }

    /**
     * Returns the [TemplateId] this [InteractiveSynchronousEvaluation] is currently pointing to.
     *
     * @return [TemplateId]
     */
    fun getCurrentTemplateId(): TemplateId = this.templates[this.templateIndex]

    /**
     * Returns the [DbTaskTemplate] this [InteractiveSynchronousEvaluation] is currently pointing to.
     *
     * Requires an active database transaction.
     *
     * @return [DbTaskTemplate]
     */
    fun getCurrentTemplate(): DbTaskTemplate = this.evaluation.template.tasks.filter {
        it.id eq this@InteractiveSynchronousEvaluation.getCurrentTemplateId()
    }.first()

    /**
     * Moves this [InteractiveSynchronousEvaluation] to the given task index.
     *
     * @param index The new task index to move to.
     */
    fun goTo(index: Int) {
        if (index < 0) throw IndexOutOfBoundsException("The template index must be greater or equal to zero.")
        if (index >= this.templates.size) throw IndexOutOfBoundsException("The template index cannot exceed the number of templates.")
        this.templateIndex = index
    }

    override fun toString(): String = "InteractiveSynchronousCompetition(id=$id, name=${name})"

    /**
     * Represents a concrete [Run] of a [DbTaskTemplate]. [DbTask]s always exist within a [InteractiveSynchronousEvaluation].
     * As a [InteractiveSynchronousEvaluation], [DbTask]s can be started and ended, and they can be used to register [DbSubmission]s.
     */
    inner class ISTaskRun(task: DbTask): AbstractInteractiveTask(task) {

        /**
         * Constructor used to generate an [ISTaskRun] from a [DbTaskTemplate].
         *
         * @param t [DbTaskTemplate] to generate [ISTaskRun] from.
         */
        constructor(t: DbTaskTemplate) : this(DbTask.new {
            status = DbTaskStatus.CREATED
            evaluation = this@InteractiveSynchronousEvaluation.evaluation
            template = t
        })

        /** The [InteractiveSynchronousEvaluation] this [DbTask] belongs to.*/
        override val competition: InteractiveSynchronousEvaluation
            get() = this@InteractiveSynchronousEvaluation

        /** The position of this [DbTask] within the [InteractiveSynchronousEvaluation]. */
        override val position: Int
            get() = this@InteractiveSynchronousEvaluation.tasks.indexOf(this)

        /** The [SubmissionFilter] instance used by this [ISTaskRun]. */
        override val filter: SubmissionFilter

        override val transformer: SubmissionTransformer

        /** The [CachingTaskScorer] instance used by this [ISTaskRun]. */
        override val scorer: CachingTaskScorer

        /** The total duration in milliseconds of this task. Usually determined by the [DbTaskTemplate] but can be adjusted! */
        override var duration: Long = this.template.duration

        /** */
        override val teams: List<TeamId> = this@InteractiveSynchronousEvaluation.description.teams.asSequence().map { it.teamId }.toList()

        init {
            check(this@InteractiveSynchronousEvaluation.tasks.isEmpty() || this@InteractiveSynchronousEvaluation.tasks.last().hasEnded) {
                "Cannot create a new task. Another task is currently running."
            }
            (this@InteractiveSynchronousEvaluation.tasks).add(this)

            /* Initialize submission filter. */
            if (this.template.taskGroup.type.submission.isEmpty) {
                this.filter = AllSubmissionFilter
            } else {
                this.filter = SubmissionFilterAggregator(
                    this.template.taskGroup.type.submission.asSequence().map { option ->
                        val parameters = this.template.taskGroup.type.configurations.query(DbConfiguredOption::key eq option.description)
                            .asSequence().map { it.key to it.value }.toMap()
                        option.newFilter(parameters)
                    }.toList()
                )
            }
            val mapToSegment = this.template.taskGroup.type.options.contains(DbTaskOption.MAP_TO_SEGMENT)
            this.transformer = if (mapToSegment) {
                SubmissionTransformerAggregator(
                    listOf(
                        SubmissionTaskMatchFilter(this.taskId),
                        MapToSegmentTransformer()
                    )
                )
            } else {
                SubmissionTaskMatchFilter(this.taskId)
            }

            /* Initialize task scorer. */
            this.scorer = CachingTaskScorer(
                when(val scoreOption = this.template.taskGroup.type.score) {
                    DbScoreOption.KIS -> KisTaskScorer(
                        this,
                        this.template.taskGroup.type.configurations.query(DbConfiguredOption::key eq scoreOption.description).asSequence().map { it.key to it.value }.toMap()
                    )
                    DbScoreOption.AVS -> AvsTaskScorer(this)
                    DbScoreOption.LEGACY_AVS -> LegacyAvsTaskScorer(this)
                    else -> throw IllegalStateException("The task score option $scoreOption is currently not supported.")
                }
            )
        }
    }
}

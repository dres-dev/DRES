package dev.dres.data.model.run

import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
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
import dev.dres.run.filter.basics.AcceptAllSubmissionFilter
import dev.dres.run.filter.basics.SubmissionFilter
import dev.dres.run.filter.basics.CombiningSubmissionFilter
import dev.dres.run.score.scoreboard.MaxNormalizingScoreBoard
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.score.scorer.AvsTaskScorer
import dev.dres.run.score.scorer.CachingTaskScorer
import dev.dres.run.score.scorer.KisTaskScorer
import dev.dres.run.score.scorer.LegacyAvsTaskScorer
import dev.dres.run.transformer.MapToSegmentTransformer
import dev.dres.run.transformer.SubmissionTaskMatchTransformer
import dev.dres.run.transformer.basics.SubmissionTransformer
import dev.dres.run.transformer.basics.CombiningSubmissionTransformer
import jetbrains.exodus.database.TransientEntityStore
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
class InteractiveSynchronousEvaluation(store: TransientEntityStore, evaluation: DbEvaluation) :
    AbstractEvaluation(store, evaluation) {

    init {
        store.transactional(true) { require(this.dbEvaluation.type == DbEvaluationType.INTERACTIVE_SYNCHRONOUS) { "Incompatible competition type ${this.dbEvaluation.type}. This is a programmer's error!" } }
        require(this.template.tasks.isNotEmpty()) { "Cannot create a run from a competition that doesn't have any tasks." }
        require(this.template.teams.isNotEmpty()) { "Cannot create a run from a competition that doesn't have any teams." }
    }

    /** List of [TaskRun]s registered for this [InteractiveSynchronousEvaluation]. */
    override val tasks = LinkedList<ISTaskRun>()


    private val templates = this.template.tasks

    /** Returns the last [TaskRun]. */
    val currentTask: AbstractInteractiveTask?
        get() = this.tasks.lastOrNull { it.templateId == this.templates[this.templateIndex].id }

    /** The index of the task template this [InteractiveSynchronousEvaluation] is pointing to. */
    var templateIndex: Int = 0
        private set

    /** List of [Scoreboard]s maintained by this [NonInteractiveEvaluation]. */
    override val scoreboards: List<Scoreboard>

    init {
        /* Load all ongoing tasks. */
        this.dbEvaluation.tasks.asSequence().forEach { ISTaskRun(it) }

        /* Prepare the evaluation scoreboards. */
        val teams = this.template.teams.asSequence().map { it.teamId }.toList()
        this.scoreboards = this.template.taskGroups.asSequence().map { group ->
            MaxNormalizingScoreBoard(group.name, this, teams, { task -> task.taskGroup == group.name }, group.name)
        }.toList()
    }

    /**
     * Returns the [TemplateId] this [InteractiveSynchronousEvaluation] is currently pointing to.
     *
     * @return [TemplateId]
     */
    fun getCurrentTemplateId(): TemplateId = this.currentTask?.templateId!!

    /**
     * Returns the [ApiTaskTemplate] this [InteractiveSynchronousEvaluation] is currently pointing to.
     *
     * Requires an active database transaction.
     *
     * @return [ApiTaskTemplate]
     */
    fun getCurrentTemplate(): ApiTaskTemplate = this.templates[this.templateIndex]

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
    inner class ISTaskRun(task: DbTask) : AbstractInteractiveTask(store, task) {

        /** The [InteractiveSynchronousEvaluation] this [DbTask] belongs to.*/
        override val evaluationRun: InteractiveSynchronousEvaluation
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
        override val teams: List<TeamId> =
            this@InteractiveSynchronousEvaluation.template.teams.map { it.teamId }.toList()

        init {

            check(this@InteractiveSynchronousEvaluation.tasks.isEmpty() || this@InteractiveSynchronousEvaluation.tasks.last().hasEnded) {
                "Cannot create a new task. Another task is currently running."
            }
            (this@InteractiveSynchronousEvaluation.tasks).add(this)

            /* Initialize submission filter. */
            this.filter = store.transactional {
                if (task.template.taskGroup.type.submission.isEmpty) {
                    AcceptAllSubmissionFilter
                } else {
                    CombiningSubmissionFilter(
                        task.template.taskGroup.type.submission.asSequence().map { option ->
                            val parameters =
                                task.template.taskGroup.type.configurations.query(DbConfiguredOption::key eq option.description)
                                    .asSequence().map { it.key to it.value }.toMap()
                            option.newFilter(parameters)
                        }.toList()
                    )
                }
            }

            this.transformer = store.transactional {
                val mapToSegment = task.template.taskGroup.type.options.contains(DbTaskOption.MAP_TO_SEGMENT)
                if (mapToSegment) {
                    CombiningSubmissionTransformer(
                        listOf(
                            SubmissionTaskMatchTransformer(this.taskId),
                            MapToSegmentTransformer()
                        )
                    )
                } else {
                    SubmissionTaskMatchTransformer(this.taskId)
                }
            }

            /* Initialize task scorer. */
            this.scorer = store.transactional {
                CachingTaskScorer(
                    when (val scoreOption = task.template.taskGroup.type.score) {
                        DbScoreOption.KIS -> KisTaskScorer(
                            this,
                            task.template.taskGroup.type.configurations.query(DbConfiguredOption::key eq scoreOption.description)
                                .asSequence().map { it.key to it.value }.toMap(),
                            store
                        )

                        DbScoreOption.AVS -> AvsTaskScorer(this, store)
                        DbScoreOption.LEGACY_AVS -> LegacyAvsTaskScorer(this, store)
                        else -> throw IllegalStateException("The task score option $scoreOption is currently not supported.")
                    }
                )
            }
        }
    }
}

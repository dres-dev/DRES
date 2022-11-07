package dev.dres.data.model.run

import dev.dres.data.model.template.EvaluationTemplate
import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.data.model.submissions.Submission
import dev.dres.run.audit.AuditLogger
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.score.interfaces.TeamTaskScorer
import dev.dres.run.validation.interfaces.SubmissionValidator
import kotlinx.dnq.query.*
import java.util.*

/**
 * Represents a concrete, interactive and synchronous [Run] of a [EvaluationTemplate].
 *
 * [InteractiveSynchronousEvaluation]s can be started, ended and they can be used to create new [TaskRun]s and access the current [TaskRun].
 *
 * @author Ralph Gasser
 * @param 1.3.0
 */
class InteractiveSynchronousEvaluation(evaluation: Evaluation) : AbstractEvaluation(evaluation) {

    init {
        require(this.evaluation.type == RunType.INTERACTIVE_SYNCHRONOUS) { "Incompatible competition type ${this.evaluation.type}. This is a programmer's error!" }
        require(this.description.tasks.size() > 0) { "Cannot create a run from a competition that doesn't have any tasks." }
        require(this.description.teams.size() > 0) { "Cannot create a run from a competition that doesn't have any teams." }
    }

    /** List of [TaskRun]s registered for this [InteractiveSynchronousEvaluation]. */
    override val tasks: List<TaskRun> = this.evaluation.tasks.asSequence().map {
        ISTaskRun(it)
    }.toMutableList()

    /** Reference to the currently active [TaskTemplate]. This is part of the task navigation. */
    var currentTaskTemplate = this.description.tasks.first()
        private set

    /** Returns the last [TaskRun]. */
    val currentTask: TaskRun?
        get() = this.tasks.firstOrNull { it.template.id == this.currentTaskTemplate.id }

    override fun toString(): String = "InteractiveSynchronousCompetition(id=$id, name=${name})"

    /**
     * Moves this [InteractiveSynchronousEvaluation] to the given task index.
     *
     * @param index The new task index to move to.
     */
    fun goTo(index: Int) {
        this.currentTaskTemplate = this.description.tasks.drop(index).first()
    }

    /**
     * Represents a concrete [Run] of a [TaskTemplate]. [Task]s always exist within a [InteractiveSynchronousEvaluation].
     * As a [InteractiveSynchronousEvaluation], [Task]s can be started and ended and they can be used to register [Submission]s.
     *
     * @version 1.2.0
     * @author Ralph Gasser
     */
    inner class ISTaskRun(task: Task): AbstractInteractiveTask(task) {

        /**
         * Constructor used to generate an [ISTaskRun] from a [TaskTemplate].
         *
         * @param description [TaskTemplate] to generate [ISTaskRun] from.
         */
        constructor(description: TaskTemplate) : this(Task.new {
            this.id = UUID.randomUUID().toString()
            this.type = RunType.INTERACTIVE_SYNCHRONOUS
            this.evaluation = this@InteractiveSynchronousEvaluation.evaluation
            this.started = System.currentTimeMillis()
            this.template = description
        })

        /** The [InteractiveSynchronousEvaluation] this [Task] belongs to.*/
        override val competition: InteractiveSynchronousEvaluation
            get() = this@InteractiveSynchronousEvaluation

        /** The position of this [Task] within the [InteractiveSynchronousEvaluation]. */
        override val position: Int
            get() = this@InteractiveSynchronousEvaluation.tasks.indexOf(this)

        /** Reference to the [TaskTemplate] describing this [Task]. */
        override val template: TaskTemplate
            get() = this.task.template

        /** The [SubmissionFilter] instance used by this [ISTaskRun]. */
        override val filter: SubmissionFilter = this.template.newFilter()

        /** The [TeamTaskScorer] instance used by this [ISTaskRun]. */
        override val scorer: TeamTaskScorer = this.template.newScorer() as? TeamTaskScorer
            ?: throw IllegalArgumentException("Specified scorer is not of type TeamTaskScorer. This is a programmer's error!")

        /** The [SubmissionValidator] used by this [ISTaskRun]. */
        override val validator: SubmissionValidator = newValidator()

        /** The total duration in milliseconds of this task. Usually determined by the [TaskTemplate] but can be adjusted! */
        override var duration: Long = this.template.duration

        init {
            check(this@InteractiveSynchronousEvaluation.tasks.isEmpty() || this@InteractiveSynchronousEvaluation.tasks.last().hasEnded) {
                "Cannot create a new task. Another task is currently running."
            }
            check(this.task.type == RunType.INTERACTIVE_SYNCHRONOUS) {
                "Incompatible competition type ${this.task.type}. This is a programmer's error!"
            }
            (this@InteractiveSynchronousEvaluation.tasks as MutableList<TaskRun>).add(this)
        }

        /**
         * Adds a [Submission] to this [Task].
         *
         * @param submission The [Submission] to add.
         */
        @Synchronized
        override fun postSubmission(submission: Submission) {
            check(this.isRunning) { "Task run '${this@InteractiveSynchronousEvaluation.name}.${this.position}' is currently not running. This is a programmer's error!" }
            check(this@InteractiveSynchronousEvaluation.description.teams.filter { it eq submission.team }.any()) {
                "Team ${submission.team.teamId} does not exists for competition run ${this@InteractiveSynchronousEvaluation.name}. This is a programmer's error!"
            }

            /* Execute submission filters. */
            this.filter.acceptOrThrow(submission)

            /* Process Submission. */
            this.submissions.add(submission)
            this.validator.validate(submission)
            AuditLogger.validateSubmission(submission, this.validator)
        }
    }
}
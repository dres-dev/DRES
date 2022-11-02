package dev.dres.data.model.run

import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.task.TaskDescription
import dev.dres.data.model.competition.task.TaskDescriptionId
import dev.dres.data.model.run.InteractiveSynchronousCompetition.Task
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.submissions.Submission
import dev.dres.run.audit.AuditLogger
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.score.interfaces.TeamTaskScorer
import dev.dres.run.validation.interfaces.SubmissionValidator
import kotlinx.dnq.query.*
import java.util.*

/**
 * Represents a concrete, interactive and synchronous [Run] of a [CompetitionDescription].
 *
 * [InteractiveSynchronousCompetition]s can be started and ended and they can be used to create new [Task]s and access the current [Task].
 *
 * @author Ralph Gasser
 * @param 1.3.0
 */
class InteractiveSynchronousCompetition(competition: Competition) : AbstractCompetitionRun(competition) {

    init {
        require(this.competition.type == RunType.INTERACTIVE_SYNCHRONOUS) { "Incompatible competition type ${this.competition.type}. This is a programmer's error!" }
        require(this.description.tasks.size() > 0) { "Cannot create a run from a competition that doesn't have any tasks." }
        require(this.description.teams.size() > 0) { "Cannot create a run from a competition that doesn't have any teams." }
    }

    /** List of [Task]s registered for this [InteractiveSynchronousCompetition]. */
    override val tasks: List<Task> = LinkedList<Task>()

    /** Reference to the currently active [TaskDescription]. This is part of the task navigation. */
    var currentTaskDescription = this.description.tasks.first()
        private set

    /** Returns the last [Task]. */
    val currentTask: Task?
        get() = this.tasks.findLast { it.description.id == this.currentTaskDescription.id }

    override fun toString(): String = "InteractiveSynchronousCompetition(id=$id, name=${name})"

    /**
     * Moves this [InteractiveSynchronousCompetition] to the given task index.
     *
     * @param index The new task index to move to.
     */
    fun goTo(index: Int) {
        this.currentTaskDescription = this.description.tasks.drop(index).first()
    }

    /**
     * Represents a concrete [Run] of a [TaskDescription]. [Task]s always exist within a [InteractiveSynchronousCompetition].
     * As a [InteractiveSynchronousCompetition], [Task]s can be started and ended and they can be used to register [Submission]s.
     *
     * @version 1.2.0
     * @author Ralph Gasser
     */
    inner class TaskRun(task: Task): AbstractInteractiveTask(task) {

        internal constructor(uid: TaskId, taskId: TaskDescriptionId, started: Long, ended: Long) : this(uid, taskId) {
            this.started = if (started == -1L) {
                null
            } else {
                started
            }
            this.ended = if (ended == -1L) {
                null
            } else {
                ended
            }
        }

        /** The [InteractiveSynchronousCompetition] this [Task] belongs to.*/
        override val competition: InteractiveSynchronousCompetition
            get() = this@InteractiveSynchronousCompetition

        /** The position of this [Task] within the [InteractiveSynchronousCompetition]. */
        override val position: Int
            get() = this@InteractiveSynchronousCompetition.tasks.indexOf(this.task)

        /** Reference to the [TaskDescription] describing this [Task]. */
        override val description: TaskDescription
            get() = this.task.description

        @Transient
        override val filter: SubmissionFilter = this.description.newFilter()

        @Transient
        override val scorer: TeamTaskScorer = this.description.newScorer() as? TeamTaskScorer
            ?: throw IllegalArgumentException("Specified scorer is not of type TeamTaskScorer. This is a programmer's error!")

        @Transient
        override val validator: SubmissionValidator = newValidator()

        /** The total duration in milliseconds of this task. Usually determined by the [TaskDescription] but can be adjusted! */
        override var duration: Long = this.description.duration

        init {
            check(this@InteractiveSynchronousCompetition.tasks.isEmpty() || this@InteractiveSynchronousCompetition.tasks.last().hasEnded) {
                "Cannot create a new task. Another task is currently running."
            }
            (this@InteractiveSynchronousCompetition.tasks as MutableList<Task>).add(this)
        }

        /**
         * Adds a [Submission] to this [Task].
         *
         * @param submission The [Submission] to add.
         */
        @Synchronized
        override fun postSubmission(submission: Submission) {
            check(this.isRunning) { "Task run '${this@InteractiveSynchronousCompetition.name}.${this.position}' is currently not running. This is a programmer's error!" }
            check(this@InteractiveSynchronousCompetition.description.teams.filter { it eq submission.team }.any()) {
                "Team ${submission.teamId} does not exists for competition run ${this@InteractiveSynchronousCompetition.name}. This is a programmer's error!"
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
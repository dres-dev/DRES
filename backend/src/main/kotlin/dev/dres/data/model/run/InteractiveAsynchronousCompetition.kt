package dev.dres.data.model.run

import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.competition.TaskDescriptionId
import dev.dres.data.model.competition.TeamId
import dev.dres.data.model.run.interfaces.Competition
import dev.dres.data.model.run.interfaces.CompetitionId
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.submissions.Submission
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.score.interfaces.TaskRunScorer
import dev.dres.run.validation.interfaces.SubmissionValidator
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a concrete, interactive and asynchronous [Run] of a [CompetitionDescription].
 *
 * [InteractiveAsynchronousCompetition]s can be started and ended and they can be used to create new [Task]s and access the current [Task].
 *
 * @author Ralph Gasser
 * @param 1.0.0
 */
class InteractiveAsynchronousCompetition(override var id: CompetitionId, override val name: String, override val description: CompetitionDescription): AbstractRun(), Competition {

    init {
        require(description.tasks.size > 0) { "Cannot create a run from a competition that doesn't have any tasks. "}
        require(description.teams.size > 0) { "Cannot create a run from a competition that doesn't have any teams. "}
    }

    /** A [ConcurrentHashMap] that maps a list of [Task]s to the [TeamId]s they belong to.*/
    private val tasksMap = ConcurrentHashMap<TeamId,MutableList<Task>>()

    /** A [List] of all active [Task]s.*/
    override val tasks: List<dev.dres.data.model.run.interfaces.Task>
        get() = this.tasksMap.values.flatten()

    /**
     * Generates and returns a [String] representation for this [InteractiveAsynchronousCompetition].
     */
    override fun toString(): String = "InteractiveAsynchronousCompetition(id=$id, name=${name})"

    /**
     * A [AbstractInteractiveTask] that takes place as part of the [InteractiveAsynchronousCompetition].
     *
     * @author Ralph Gasser
     * @version 1.0.0
     */
    inner class Task internal constructor (override val uid: TaskId = UID(), val teamId: TeamId, val taskDescriptionId: TaskDescriptionId): AbstractInteractiveTask() {

        internal constructor(uid: TaskId, teamId: TeamId, taskId: TaskDescriptionId, started: Long, ended: Long): this(uid, teamId, taskId) {
            this.started =  if (started == -1L) { null } else { started }
            this.ended = if (ended == -1L) { null } else { ended }
        }

        /** The [InteractiveAsynchronousCompetition] this [Task] belongs to.*/
        override val competition: InteractiveAsynchronousCompetition
            get() = this@InteractiveAsynchronousCompetition

        /** The position of this [Task] within the [InteractiveAsynchronousCompetition]. */
        override val position: Int
            get() = this@InteractiveAsynchronousCompetition.tasksMap[this.teamId]?.indexOf(this) ?: -1

        @Transient
        override val description: TaskDescription = this@InteractiveAsynchronousCompetition.description.tasks.find { it.id == this.taskDescriptionId }
            ?: throw IllegalArgumentException("Task with taskId ${this.taskDescriptionId} not found.")

        @Transient
        override val filter: SubmissionFilter = this.description.newFilter()

        @Transient
        override val scorer: TaskRunScorer = this.description.newScorer()

        @Transient
        override val validator: SubmissionValidator = this.newValidator()

        /** The total duration in milliseconds of this task. Usually determined by the [TaskDescription] but can be adjusted! */
        override var duration: Long = this.description.duration

        init {
            check(this@InteractiveAsynchronousCompetition.description.teams.any { it.uid == this.teamId }) {
                "Cannot start a new task run for team with ID ${this.teamId}. Team is not registered for competition."
            }
            this@InteractiveAsynchronousCompetition.tasksMap.compute(this.teamId) { _, v ->
                val list = v ?: LinkedList<Task>()
                check(list.isEmpty() || list.last().hasEnded) { "Cannot create a new task. Another task is currently running." }
                list.add(this)
                list
            }
        }

        /**
         * Adds a [Submission] to this [InteractiveAsynchronousCompetition.Task].
         *
         * @param submission The [Submission] to add.
         * @throws IllegalArgumentException If [Submission] could not be added for any reason.
         */
        @Synchronized
        override fun addSubmission(submission: Submission) {
            check(!this.isRunning) { "Task run '${this@InteractiveAsynchronousCompetition.name}.${this.position}' is currently not running." }
            check(this.teamId == submission.teamId) { "Team ${submission.teamId} is not eligible to submit to this task." }
            check(this.filter.test(submission)) { "The provided submission $submission was rejected by the filter." }

            /* Process Submission. */
            (this.submissions as MutableList).add(submission)
            this.validator.validate(submission)
        }
    }
}
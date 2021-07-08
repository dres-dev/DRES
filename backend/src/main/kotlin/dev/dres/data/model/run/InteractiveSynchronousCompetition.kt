package dev.dres.data.model.run

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.competition.TaskDescriptionId
import dev.dres.data.model.run.InteractiveSynchronousCompetition.Task
import dev.dres.data.model.run.interfaces.Competition
import dev.dres.data.model.run.interfaces.CompetitionId
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.submissions.Submission
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.score.interfaces.TeamTaskScorer
import dev.dres.run.validation.interfaces.SubmissionValidator
import java.util.*


/**
 * Represents a concrete, interactive and synchronous [Run] of a [CompetitionDescription].
 *
 * [InteractiveSynchronousCompetition]s can be started and ended and they can be used to create new [Task]s and access the current [Task].
 *
 * @author Ralph Gasser
 * @param 1.3.0
 */
class InteractiveSynchronousCompetition(override var id: CompetitionId, override val name: String, override val description: CompetitionDescription): AbstractRun(), Competition {

    internal constructor(id: CompetitionId, name: String, competitionDescription: CompetitionDescription, started: Long, ended: Long) : this(id, name, competitionDescription) {
        this.started = if (started == -1L) { null } else { started }
        this.ended = if (ended == -1L) { null } else { ended }
    }

    init {
        require(this.description.tasks.size > 0) { "Cannot create a run from a competition that doesn't have any tasks. "}
        require(this.description.teams.size > 0) { "Cannot create a run from a competition that doesn't have any teams. "}
    }

    /** List of [Task]s registered for this [InteractiveSynchronousCompetition]. */
    override val tasks: List<Task> = LinkedList<Task>()

    /** Returns the last [Task]. */
    @get:JsonIgnore
    val lastTask: Task?
        get() = this.tasks.lastOrNull()

    override fun toString(): String = "InteractiveSynchronousCompetition(id=$id, name=${name})"

    /**
     * Represents a concrete [Run] of a [TaskDescription]. [Task]s always exist within a [InteractiveSynchronousCompetition].
     * As a [InteractiveSynchronousCompetition], [Task]s can be started and ended and they can be used to register [Submission]s.
     *
     * @version 1.2.0
     * @author Ralph Gasser
     */
    @JsonIgnoreProperties(value = ["competition"])
    inner class Task(override val uid: TaskId = UID(), val taskDescriptionId: TaskDescriptionId): Run, AbstractInteractiveTask() {

        internal constructor(uid: TaskId, taskId: TaskDescriptionId, started: Long, ended: Long): this(uid, taskId) {
            this.started =  if (started == -1L) { null } else { started }
            this.ended = if (ended == -1L) { null } else { ended }
        }

        /** The [InteractiveSynchronousCompetition] this [Task] belongs to.*/
        override val competition: InteractiveSynchronousCompetition
            get() = this@InteractiveSynchronousCompetition

        /** The position of this [Task] within the [InteractiveSynchronousCompetition]. */
        override val position: Int
            get() = this@InteractiveSynchronousCompetition.tasks.indexOf(this)

        /** Reference to the [TaskDescription] describing this [Task]. */
        @Transient
        override val description: TaskDescription = this@InteractiveSynchronousCompetition.description.tasks.find { it.id == this.taskDescriptionId } ?: throw IllegalArgumentException("There is no task with ID ${this.taskDescriptionId}.")

        @Transient
        override val filter: SubmissionFilter = description.newFilter()

        @Transient
        override val scorer: TeamTaskScorer = this.description.newScorer() as? TeamTaskScorer ?: throw IllegalArgumentException("specified scorer is not of type TeamTaskScorer")

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
        override fun addSubmission(submission: Submission) {
            check(this.isRunning) { "Task run '${this@InteractiveSynchronousCompetition.name}.${this.position}' is currently not running. This is a programmer's error!" }
            check(this@InteractiveSynchronousCompetition.description.teams.any { it.uid == submission.teamId }) {
                "Team ${submission.teamId} does not exists for competition run ${this@InteractiveSynchronousCompetition.name}. This is a programmer's error!"
            }

            /* Execute submission filters. */
            this.filter.acceptOrThrow(submission)

            /* Process Submission. */
            this.submissions.add(submission)
            this.validator.validate(submission)
        }
    }
}
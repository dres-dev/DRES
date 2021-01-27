package dev.dres.data.model.run

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import dev.dres.data.model.Entity
import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.competition.TaskDescriptionId
import dev.dres.data.model.run.CompetitionRun.TaskRun
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.score.interfaces.TaskRunScorer
import dev.dres.run.validation.interfaces.SubmissionValidator
import java.util.*

typealias CompetitionRunId = UID
typealias TaskRunId = UID

/**
 * Represents a concrete [Run] of a [CompetitionDescription]. [CompetitionRun]s can be started and
 * ended and they can be used to create new [TaskRun]s and access the current [TaskRun].
 *
 * @author Ralph Gasser
 * @param 1.2.1
 */
open class CompetitionRun(override var id: CompetitionRunId, val name: String, val competitionDescription: CompetitionDescription): Run, Entity {

    internal constructor(id: CompetitionRunId, name: String, competitionDescription: CompetitionDescription, started: Long, ended: Long) : this(id, name, competitionDescription) {
        this.started = if (started == -1L) { null } else { started }
        this.ended = if (ended == -1L) { null } else { ended }
    }

    init {
        require(competitionDescription.tasks.size > 0) { "Cannot create a run from a competition that doesn't have any tasks. "}
        require(competitionDescription.teams.size > 0) { "Cannot create a run from a competition that doesn't have any teams. "}
    }

    /** Timestamp of when this [CompetitionRun] was started. */
    @Volatile
    override var started: Long? = null
        protected set

    /** Timestamp of when this [TaskRun] was ended. */
    @Volatile
    override var ended: Long? = null
        protected set

    /** List of [TaskRun]s registered for this [CompetitionRun]. */
    val runs: List<TaskRun> = LinkedList<TaskRun>()

    /** Returns the last [TaskRun]. */
    val lastTask: TaskRun?
        get() = this.runs.lastOrNull()

    /**
     * Starts this [CompetitionRun].
     */
    open fun start() {
        if (this.hasStarted) {
            throw IllegalStateException("Competition run '$name' has already been started.")
        }
        this.started = System.currentTimeMillis()
    }

    /**
     * Ends this [CompetitionRun].
     */
    open fun end() {
        if (!this.isRunning) {
            this.started = System.currentTimeMillis()
        }
        this.ended = System.currentTimeMillis()
    }

    /**
     * Creates a new [TaskRun] for the given [TaskDescription].
     *
     * @param taskId [UID] of the [TaskDescription] to start a [TaskRun] for.
     */
    fun newTaskRun(taskId: TaskDescriptionId): TaskRun {
        if (this@CompetitionRun.runs.isEmpty() || this@CompetitionRun.runs.last().hasEnded) {
            val ret = TaskRun(taskId = taskId)
            (this.runs as MutableList<TaskRun>).add(ret)
            return ret
        } else {
            throw IllegalStateException("Another Task is currently running.")
        }
    }

    override fun toString(): String = "CompetitionRun(id=$id, name=${name})"




    /**
     * Represents a concrete [Run] of a [TaskDescription]. [TaskRun]s always exist within a [CompetitionRun].
     * As a [CompetitionRun], [TaskRun]s can be started and ended and they can be used to register [Submission]s.
     *
     * @version 1.2.0
     * @author Ralph Gasser
     */
    @JsonIgnoreProperties(value = ["competition"])
    inner class TaskRun (val uid: TaskRunId = UID(), val taskId: UID): Run {

        internal constructor(uid: TaskRunId, taskId: TaskDescriptionId, started: Long, ended: Long): this(uid, taskId) {
            this.started =  if (started == -1L) { null } else { started }
            this.ended = if (ended == -1L) { null } else { ended }
        }

        /** List of [Submission]s* registered for this [TaskRun]. */
        val submissions: List<Submission> = mutableListOf()

        /** The [CompetitionRun] this [TaskRun] belongs to.*/
        val competition: CompetitionRun
            get() = this@CompetitionRun

        /** The position of this [TaskRun] within the [CompetitionRun]. */
        val position: Int
            get() = this@CompetitionRun.runs.indexOf(this)

        /** Reference to the [TaskDescription] describing this [TaskRun]. */
        @Transient
        val task: TaskDescription = this@CompetitionRun.competitionDescription.tasks.find { it.id == this.taskId } ?: throw IllegalArgumentException("There is no task with ID ${this.taskId}.")

        /** The [SubmissionFilter] used to filter [Submission]s. */
        @Transient
        val filter: SubmissionFilter = this.task.newFilter()

        /** The [TaskRunScorer] used to update score for this [TaskRun]. */
        @Transient
        val scorer: TaskRunScorer = this.task.newScorer()

        /** The [SubmissionValidator] used to validate [Submission]s. */
        @Transient
        val validator: SubmissionValidator = this.task.newValidator()

        /** Timestamp of when this [TaskRun] was started. */
        @Volatile
        override var started: Long? = null
            private set

        /** Timestamp of when this [TaskRun] was ended. */
        @Volatile
        override var ended: Long? = null
            private set

        /** Duration of this [TaskRun]. Defaults to the duration specified in the [TaskDescription]. */
        @Volatile
        var duration: Long = this.task.duration

        /**
         * Starts this [CompetitionRun.TaskRun].
         */
        internal fun start() {
            if (this.hasStarted) {
                throw IllegalStateException("Task run '${this@CompetitionRun.name}.${this.position}' has already been started.")
            }
            this.started = System.currentTimeMillis()
        }

        /**
         * Ends this [CompetitionRun.TaskRun].
         */
        internal fun end() {
            if (!this.isRunning) {
                val end = System.currentTimeMillis()
                this.started = end
                this.ended = end
            } else {
                this.ended = System.currentTimeMillis()
            }
        }

        /**
         * Adds a [Submission] to this [TaskRun].
         *
         * @param submission The [Submission] to add.
         */
        @Synchronized
        fun addSubmission(submission: Submission) {
            if (!this.isRunning) {
                throw IllegalStateException("Task run '${this@CompetitionRun.name}.${this.position}' is currently not running.")
            }
            if (!this@CompetitionRun.competitionDescription.teams.any { it.uid == submission.teamId }) {
                throw IllegalStateException("Team ${submission.teamId} does not exists for competition run ${this@CompetitionRun.name}.")
            }
            if (!this.filter.test(submission)) {
                throw IllegalArgumentException("The provided submission $submission was rejected.")
            }

            /* Process Submission. */
            (this.submissions as MutableList).add(submission)
            this.validator.validate(submission)
        }
    }
}
package dev.dres.data.model.run

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.competition.TaskDescriptionId
import dev.dres.data.model.run.InteractiveCompetitionRun.TaskRun
import java.util.*


/**
 * Represents a concrete [Run] of a [CompetitionDescription]. [InteractiveCompetitionRun]s can be started and
 * ended and they can be used to create new [TaskRun]s and access the current [TaskRun].
 *
 * @author Ralph Gasser
 * @param 1.2.1
 */
open class InteractiveCompetitionRun(override var id: CompetitionRunId, name: String, competitionDescription: CompetitionDescription): CompetitionRun(id, name, competitionDescription) {

    internal constructor(id: CompetitionRunId, name: String, competitionDescription: CompetitionDescription, started: Long, ended: Long) : this(id, name, competitionDescription) {
        this.started = if (started == -1L) { null } else { started }
        this.ended = if (ended == -1L) { null } else { ended }
    }

    init {
        require(competitionDescription.tasks.size > 0) { "Cannot create a run from a competition that doesn't have any tasks. "}
        require(competitionDescription.teams.size > 0) { "Cannot create a run from a competition that doesn't have any teams. "}
    }

    /** List of [TaskRun]s registered for this [InteractiveCompetitionRun]. */
    override val tasks: List<TaskRun> = LinkedList<TaskRun>()

    /** Returns the last [TaskRun]. */
    val lastTask: TaskRun?
        get() = this.tasks.lastOrNull()

    /**
     * Creates a new [TaskRun] for the given [TaskDescription].
     *
     * @param taskId [UID] of the [TaskDescription] to start a [TaskRun] for.
     */
    fun newTaskRun(taskId: TaskDescriptionId): TaskRun {
        if (this@InteractiveCompetitionRun.tasks.isEmpty() || this@InteractiveCompetitionRun.tasks.last().hasEnded) {
            val ret = TaskRun(taskDescriptionId = taskId)
            (this.tasks as MutableList<TaskRun>).add(ret)
            return ret
        } else {
            throw IllegalStateException("Another Task is currently running.")
        }
    }

    override fun toString(): String = "InteractiveCompetitionRun(id=$id, name=${name})"


    /**
     * Represents a concrete [Run] of a [TaskDescription]. [TaskRun]s always exist within a [InteractiveCompetitionRun].
     * As a [InteractiveCompetitionRun], [TaskRun]s can be started and ended and they can be used to register [Submission]s.
     *
     * @version 1.2.0
     * @author Ralph Gasser
     */
    @JsonIgnoreProperties(value = ["competition"])
    inner class TaskRun (override val uid: TaskId = UID(), val taskDescriptionId: TaskDescriptionId): Run, InteractiveTask() {

        internal constructor(uid: TaskId, taskId: TaskDescriptionId, started: Long, ended: Long): this(uid, taskId) {
            this.started =  if (started == -1L) { null } else { started }
            this.ended = if (ended == -1L) { null } else { ended }
        }

        /** List of [Submission]s* registered for this [TaskRun]. */
        val submissions: List<Submission> = mutableListOf()

        /** The [InteractiveCompetitionRun] this [TaskRun] belongs to.*/
        val competition: InteractiveCompetitionRun
            get() = this@InteractiveCompetitionRun

        /** The position of this [TaskRun] within the [InteractiveCompetitionRun]. */
        val position: Int
            get() = this@InteractiveCompetitionRun.tasks.indexOf(this)

        /** Reference to the [TaskDescription] describing this [TaskRun]. */
        @Transient
        override val taskDescription: TaskDescription = this@InteractiveCompetitionRun.competitionDescription.tasks.find { it.id == this.taskDescriptionId } ?: throw IllegalArgumentException("There is no task with ID ${this.taskDescriptionId}.")

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
        var duration: Long = this.taskDescription.duration

        /**
         * Starts this [InteractiveCompetitionRun.TaskRun].
         */
        internal fun start() {
            if (this.hasStarted) {
                throw IllegalStateException("Task run '${this@InteractiveCompetitionRun.name}.${this.position}' has already been started.")
            }
            this.started = System.currentTimeMillis()
        }

        /**
         * Ends this [InteractiveCompetitionRun.TaskRun].
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
        override fun addSubmission(submission: Submission) {
            if (!this.isRunning) {
                throw IllegalStateException("Task run '${this@InteractiveCompetitionRun.name}.${this.position}' is currently not running.")
            }
            if (!this@InteractiveCompetitionRun.competitionDescription.teams.any { it.uid == submission.teamId }) {
                throw IllegalStateException("Team ${submission.teamId} does not exists for competition run ${this@InteractiveCompetitionRun.name}.")
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
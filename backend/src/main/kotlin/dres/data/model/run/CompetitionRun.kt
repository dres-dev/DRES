package dres.data.model.run

import dres.data.model.Entity
import dres.data.model.competition.Competition
import kotlinx.serialization.Serializable
import java.lang.IllegalArgumentException
import java.util.*

/**
 * Represents a concrete instance or `run` of a [Competition]. [CompetitionRun]s can be started
 * and ended and they can be used to create new [TaskRun]s and access the current [TaskRun].
 *
 * @author Ralph Gasser
 * @param 1.0
 */
@Serializable
class CompetitionRun(override var id: Long, val name: String, val competition: Competition): Run, Entity {

    internal constructor(id: Long, name: String, competition: Competition, started: Long?, ended: Long?) : this(id, name, competition) {
        this.started = started
        this.ended = ended
    }

    /** Timestamp of when this [CompetitionRun] was started. */
    @Volatile
    override var started: Long? = null
        private set

    /** Timestamp of when this [TaskRun] was ended. */
    @Volatile
    override var ended: Long? = null
        private set

    /** List of [TaskRun]s registered for this [CompetitionRun]. */
    val runs: List<TaskRun> = LinkedList<TaskRun>()

    /** Returns the current [TaskRun]. */
    val currentTask: TaskRun?
        get() = this.runs.lastOrNull()

    /**
     * Starts this [CompetitionRun].
     */
    fun start() {
        if (this.hasStarted) {
            throw IllegalStateException("Competition run '$name' has already been started.")
        }
        this.started = System.currentTimeMillis()
    }

    /**
     * Ends this [CompetitionRun].
     */
    fun end() {
        if (!this.isRunning) {
            throw IllegalStateException("Competition run '$name' is not running.")
        }
        this.ended = System.currentTimeMillis()
    }

    /**
     * Creates a new [TaskRun] for the given [Task].
     *
     * @param task ID of the [Task] to start a [TaskRun]
     */
    fun newTaskRun(task: Int) {
        if (this@CompetitionRun.runs.isEmpty() || this@CompetitionRun.runs.last().hasEnded) {
            (this.runs as MutableList<TaskRun>).add(TaskRun(task))
        } else {
            throw IllegalStateException("Another Task is currently running.")
        }
    }


    /**
     * Represents a concrete instance or `run` of a [Task]. [TaskRun]s always exist within a
     * [CompetitionRun]. As a [CompetitionRun], [TaskRun]s can be started and ended and they
     * can be used to register [Submission]s.
     *
     * @version 1.0
     * @author Ralph Gasser
     */
    @Serializable
    inner class TaskRun (val task: Int): Run {

        internal constructor(task: Int, started: Long, ended: Long): this(task) {
            this.started =  if (started == -1L) { null } else { started }
            this.ended = if (ended == -1L) { null } else { ended }
        }

        /** Timestamp of when this [TaskRun] was started. */
        @Volatile
        override var started: Long? = null
            private set

        /** Timestamp of when this [TaskRun] was ended. */
        @Volatile
        override var ended: Long? = null
            private set

        /** The position of this [TaskRun] within the [CompetitionRun]. */
        val position: Int
            get() = this@CompetitionRun.runs.indexOf(this)

        /** List of [Submission]s* registered for this [TaskRun]. */
        val submissions: List<Submission> = LinkedList()

        init {
            if (this@CompetitionRun.competition.tasks.size < this.task) {
                throw IllegalArgumentException("There is no task with ID $task.")
            }
        }

        /**
         * Starts this [CompetitionRun].
         */
        fun start() {
            if (this.hasStarted) {
                throw IllegalStateException("Task run '${this@CompetitionRun.name}.${this.position}' has already been started.")
            }
            this.started = System.currentTimeMillis()
        }

        /**
         * Ends this [CompetitionRun].
         */
        fun end() {
            if (!this.isRunning) {
                throw IllegalStateException("Task run '${this@CompetitionRun.name}.${this.position}' is currently not running.")
            }
            this.ended = System.currentTimeMillis()
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
            if (this@CompetitionRun.competition.teams.size < submission.team) {
                throw IllegalStateException("Team ${submission.team} does not exists for competition run ${this@CompetitionRun.name}.")
            }
            (this.submissions as LinkedList).add(submission)
        }
    }
}
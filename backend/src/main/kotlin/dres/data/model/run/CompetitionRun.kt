package dres.data.model.run

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import dres.data.model.Entity
import dres.data.model.competition.CompetitionDescription
import dres.data.model.competition.interfaces.TaskDescription
import dres.data.model.run.CompetitionRun.TaskRun
import dres.run.filter.SubmissionFilter
import dres.run.score.interfaces.IncrementalTaskRunScorer
import dres.run.score.interfaces.RecalculatingTaskRunScorer
import dres.run.score.interfaces.TaskRunScorer
import dres.run.validation.interfaces.SubmissionValidator
import java.util.*

/**
 * Represents a concrete instance or `run` of a [CompetitionDescription]. [CompetitionRun]s can be started
 * and ended and they can be used to create new [TaskRun]s and access the current [TaskRun].
 *
 * @author Ralph Gasser
 * @param 1.0
 */
class CompetitionRun(override var id: Long, val name: String, val competitionDescription: CompetitionDescription, val uid: String = UUID.randomUUID().toString()): Run, Entity {

    internal constructor(id: Long, name: String, competitionDescription: CompetitionDescription, uid: String, started: Long, ended: Long) : this(id, name, competitionDescription, uid) {
        this.started =  if (started == -1L) { null } else { started }
        this.ended = if (ended == -1L) { null } else { ended }
    }

    init {
        require(competitionDescription.tasks.size > 0) { "Cannot create a run from a competition that doesn't have any tasks. "}
        require(competitionDescription.teams.size > 0) { "Cannot create a run from a competition that doesn't have any teams. "}
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
    override fun start() {
        if (this.hasStarted) {
            throw IllegalStateException("Competition run '$name' has already been started.")
        }
        this.started = System.currentTimeMillis()
    }

    /**
     * Ends this [CompetitionRun].
     */
    override fun end() {
        if (!this.isRunning) {
            this.started = System.currentTimeMillis()
        }
        this.ended = System.currentTimeMillis()
    }

    /**
     * Creates a new [TaskRun] for the given [Task].
     *
     * @param task ID of the [Task] to start a [TaskRun]
     */
    fun newTaskRun(task: Int): TaskRun {
        if (this@CompetitionRun.runs.isEmpty() || this@CompetitionRun.runs.last().hasEnded) {
            val ret = TaskRun(task)
            (this.runs as MutableList<TaskRun>).add(ret)
            return ret
        } else {
            throw IllegalStateException("Another Task is currently running.")
        }
    }

    override fun toString(): String = "CompetitionRun(id=$id, uid=$uid, name=${name})"

    /**
     * Represents a concrete instance or `run` of a [Task]. [TaskRun]s always exist within a
     * [CompetitionRun]. As a [CompetitionRun], [TaskRun]s can be started and ended and they
     * can be used to register [Submission]s.
     *
     * @version 1.0
     * @author Ralph Gasser
     */
    @JsonIgnoreProperties(value = ["competition"])
    inner class TaskRun (val taskId: Int, val uid: String = UUID.randomUUID().toString()): Run {

        internal constructor(task: Int, uid: String, started: Long, ended: Long): this(task, uid) {
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
        private val position: Int
            get() = this@CompetitionRun.runs.indexOf(this)

        /** Exposable data of this [TaskRun] */
        val data: TaskRunData = TaskRunData(this.task, this.taskId)

        val task: TaskDescription
            get() = this@CompetitionRun.competitionDescription.tasks[this@TaskRun.taskId]

        /** The [CompetitionRun] this [TaskRun] belongs to.*/
        val competition: CompetitionRun
            get() = this@CompetitionRun

        /** The [SubmissionFilter] used to filter [Submission]s. */
        @Transient
        val filter: SubmissionFilter = this.task.newFilter()

        /** The [TaskRunScorer] used to update score for this [TaskRun]. */
        @Transient
        val scorer: TaskRunScorer = this.task.newScorer()

        /** The [SubmissionValidator] used to validate [Submission]s. */
        @Transient
        val validator: SubmissionValidator = this.task.newValidator {
            when(this.scorer){
                is IncrementalTaskRunScorer -> this.scorer.update(it)
                is RecalculatingTaskRunScorer -> this.scorer.analyze(this)
                else -> this.scorer.scores()
            }
        }

        init {
            if (this@CompetitionRun.competitionDescription.tasks.size < this.taskId) {
                throw IllegalArgumentException("There is no task with ID $taskId.")
            }
        }

        /**
         * Starts this [CompetitionRun].
         */
        override fun start() {
            if (this.hasStarted) {
                throw IllegalStateException("Task run '${this@CompetitionRun.name}.${this.position}' has already been started.")
            }
            this.started = System.currentTimeMillis()
        }

        /**
         * Ends this [CompetitionRun].
         */
        override fun end() {
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
            if (this@CompetitionRun.competitionDescription.teams.size < submission.team) {
                throw IllegalStateException("Team ${submission.team} does not exists for competition run ${this@CompetitionRun.name}.")
            }
            if (!this.filter.test(submission)) {
                throw IllegalArgumentException("The provided submission $submission was rejected.")
            }

            /* Process Submission. */
            (this.data.submissions as MutableList).add(submission)
            this.validator.validate(submission)
        }
    }
}

class TaskRunData(val task: TaskDescription, val taskId: Int) {

    /** List of [Submission]s* registered for this [TaskRun]. */
    val submissions: List<Submission> = mutableListOf()

    constructor(task: TaskDescription, taskId: Int, submissions: List<Submission> = emptyList()): this(task, taskId) {
        (this.submissions as MutableList).addAll(submissions)
    }

    //TODO add logging information

}
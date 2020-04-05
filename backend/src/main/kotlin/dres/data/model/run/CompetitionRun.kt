package dres.data.model.run

import dres.data.model.Entity
import dres.data.model.competition.Competition
import dres.data.model.competition.Task
import dres.data.model.competition.TaskDescription
import dres.data.model.competition.TaskType
import dres.data.model.run.CompetitionRun.TaskRun
import dres.run.RunExecutor
import dres.run.validate.JudgementValidator
import dres.run.validate.SubmissionValidator
import dres.run.validate.TextualKisSubmissionValidator
import dres.run.validate.VisualKisSubmissionValidator
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
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

    private val validators = mapOf(
            TaskType.KIS_VISUAL to VisualKisSubmissionValidator(),
            TaskType.KIS_TEXTUAL to TextualKisSubmissionValidator(),
            TaskType.AVS to JudgementValidator(RunExecutor.judgementQueue)
    )

    init {
        require(competition.tasks.size > 0) { "Cannot create a run from a competition that doesn't have any tasks. "}
        require(competition.teams.size > 0) { "Cannot create a run from a competition that doesn't have any teams. "}
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

    @kotlinx.serialization.Transient
    val awaitingValidation = mutableListOf<Deferred<Pair<Submission, SubmissionStatus>>>()

    val hasUnvalidatedSubmissions: Boolean
        get() = awaitingValidation.isNotEmpty()

    /**
     * Checks if new submission validations are available and updates [Submission]s accordingly
     */
    @ExperimentalCoroutinesApi
    fun updateSubmissionValidations() {
        if (!hasUnvalidatedSubmissions) {
            return
        }
        val completed = awaitingValidation.filter { it.isCompleted }
        completed.forEach {
            val result = it.getCompleted()
            result.first.status = result.second
        }
        //remove completed ones
        awaitingValidation.removeAll(completed)

        //TODO maybe trigger an update with the freshly evaluated submissions somewhere?
    }


    private fun validator(run: TaskRun): SubmissionValidator<Submission, TaskDescription> {
        return validators[run.task.description.taskType] as SubmissionValidator<Submission, TaskDescription>
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
    inner class TaskRun (val taskId: Int): Run {

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

        /** The [Task] referenced by this [TaskRun]. */
        val task: Task
            get() = this@CompetitionRun.competition.tasks[this.taskId]

        private val validator: SubmissionValidator<Submission, TaskDescription> = validator(this)

        init {
            if (this@CompetitionRun.competition.tasks.size < this.taskId) {
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

            runBlocking { //TODO specify execution context
                awaitingValidation.add(
                        async {
                           submission to validator.validate(submission, task.description)
                    }
                )
            }
        }
    }
}
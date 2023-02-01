package dev.dres.data.model.run

import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.data.model.submissions.Submission
import dev.dres.run.TaskStatus
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.validation.interfaces.SubmissionValidator
import kotlinx.dnq.util.findById
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * An abstract [Run] implementation that can be used by different subtypes.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractTask(task: Task): TaskRun {

    /** The internal [xdId] of this [AbstractEvaluation].
     *
     * Since this cannot change during the lifetime of an evaluation, it is kept in memory.
     */
    protected val xdId = task.xdId

    /**
     * Accessor for the [Task] underpinning this [AbstractTask]
     */
    protected val task: Task
        get() = Task.findById(this.xdId)

    /**
     * The [EvaluationId] of this [AbstractTask].
     *
     * Since this cannot change during the lifetime of an evaluation, it is kept in memory.
     */
    final override val id: EvaluationId = this.task.id

    /** List of [Submission]s* registered for this [AbstractTask]. */
    protected val submissions: ConcurrentLinkedQueue<Submission> = ConcurrentLinkedQueue<Submission>()

    /** Timestamp of when this [AbstractTask] was started. */
    final override var started: Long
        get() = this.task.started
        protected set(value) {
            this.task.started = value
        }

    /** Timestamp of when this [AbstractTask] was ended. */
    final override var ended: Long?
        get() = this.task.ended
        protected set(value) {
            this.task.ended = value
        }

    /** Reference to the [TaskTemplate] describing this [AbstractTask]. */
    final override val template: TaskTemplate
        get() = this.task.template

    @Volatile
    final override var status: TaskStatus = TaskStatus.CREATED
        protected set

    /** The [SubmissionFilter] used to filter [Submission]s. */
    abstract val filter: SubmissionFilter

    /** The [SubmissionValidator] used to validate [Submission]s. */
    abstract val validator: SubmissionValidator

    /**
     * Prepares this [TaskRun] for later starting.
     */
    override fun prepare() {
        if (this.hasEnded) {
            throw IllegalStateException("Run has already ended.")
        }
        if (this.hasStarted) {
            throw IllegalStateException("Run has already been started.")
        }
        this.status = TaskStatus.PREPARING
    }

    /**
     * Starts this [AbstractTask].
     */
    override fun start() {
        if (this.hasStarted) {
            throw IllegalStateException("Run has already been started.")
        }
        this.started = System.currentTimeMillis()
        this.status = TaskStatus.RUNNING
    }

    /**
     * Ends this [AbstractTask].
     */
    override fun end() {
        if (!this.isRunning) {
            this.started = System.currentTimeMillis()
        }
        this.ended = System.currentTimeMillis()
        this.status = TaskStatus.ENDED
    }

    /**
     * Reactivates this [AbstractTask].
     */
    override fun reactivate() {
        if (this.ended == null){
            throw IllegalStateException("Run has not yet ended.")
        }
        this.ended = null
        this.status = TaskStatus.RUNNING
    }

    /** Returns a [List] of all [Submission]s held by this [AbstractTask]. */
    override fun getSubmissions() = this.submissions.toList()

    /**
     * Adds a new [Submission] to this [AbstractInteractiveTask].
     *
     * @param submission The [Submission] to append.
     */
    abstract fun postSubmission(submission: Submission)
}
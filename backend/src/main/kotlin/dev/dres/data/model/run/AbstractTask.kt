package dev.dres.data.model.run

import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.template.TemplateId
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
abstract class AbstractTask(task: DbTask): TaskRun {

    /** The internal [xdId] of this [AbstractEvaluation].
     *
     * Since this cannot change during the lifetime of an evaluation, it is kept in memory.
     */
    private val xdId = task.xdId

    /**
     * Accessor for the [DbTask] underpinning this [AbstractTask]
     */
    protected val task: DbTask
        get() = DbTask.findById(this.xdId)

    /**
     * The [TaskId] of this [AbstractTask].
     *
     * Since this cannot change during the lifetime of an evaluation, it is kept in memory.
     */
    final override val id: TaskId = this.task.id

    /**
     * The [TemplateId] of this [AbstractTask].
     *
     * Since this cannot change during the lifetime of an evaluation, it is kept in memory.
     */
    final override val templateId: TemplateId = this.task.template.templateId

    /** List of [DbSubmission]s* registered for this [AbstractTask]. */
    protected val submissions: ConcurrentLinkedQueue<DbSubmission> = ConcurrentLinkedQueue<DbSubmission>()

    /**
     * Timestamp of when this [AbstractTask] was started.
     *
     * Setter requires active database transaction!
     */
    final override var started: Long? = null
        protected set(value) {
            field = value
            this.task.started = value  /* Update backing database field. */
        }

    /**
     * Timestamp of when this [AbstractTask] was ended.
     *
     * Setter requires active database transaction!
     */
    final override var ended: Long? = null
        protected set(value) {
            field = value
            this.task.ended = value /* Update backing database field. */
        }

    /**
     * Reference to the [DbTaskTemplate] describing this [AbstractTask].
     *
     * Requires active database transaction!
     */
    final override val template: DbTaskTemplate
        get() = this.task.template

    /** The current status of this [AbstractTask]. This is a transient property. */
    @Volatile
    final override var status: TaskStatus = TaskStatus.CREATED
        protected set

    /** The [SubmissionFilter] used to filter [DbSubmission]s. */
    abstract val filter: SubmissionFilter

    /** The [SubmissionValidator] used to validate [DbSubmission]s. */
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

    /** Returns a [List] of all [DbSubmission]s held by this [AbstractTask]. */
    override fun getSubmissions() = this.submissions.asSequence()

    /**
     * Adds a new [DbSubmission] to this [AbstractInteractiveTask].
     *
     * @param submission The [DbSubmission] to append.
     */
    abstract fun postSubmission(submission: Submission)
}
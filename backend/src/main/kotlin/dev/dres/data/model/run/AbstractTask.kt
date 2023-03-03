package dev.dres.data.model.run

import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.template.TemplateId
import dev.dres.run.TaskStatus
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.validation.interfaces.AnswerSetValidator
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.mapDistinct
import kotlinx.dnq.util.findById

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
    final override val taskId: TaskId = this.task.id

    /**
     * The [TemplateId] of this [AbstractTask].
     *
     * Since this cannot change during the lifetime of an evaluation, it is kept in memory.
     */
    final override val templateId: TemplateId = this.task.template.templateId

    /**
     * Timestamp of when this [AbstractTask] was started.
     *
     * Setter requires active database transaction!
     */
    final override var started: Long? = task.started
        protected set(value) {
            field = value
            this.task.started = value  /* Update backing database field. */
        }

    /**
     * Timestamp of when this [AbstractTask] was ended.
     *
     * Setter requires active database transaction!
     */
    final override var ended: Long? = task.ended
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
    final override var status: TaskStatus = when {
        this.started != null && this.ended != null -> TaskStatus.ENDED
        this.started != null && this.ended == null -> TaskStatus.RUNNING
        else -> TaskStatus.CREATED
    }
    protected set

    /** The [SubmissionFilter] used to filter [DbSubmission]s. */
    abstract val filter: SubmissionFilter

    /** The [AnswerSetValidator] used to validate [DbSubmission]s. */
    abstract val validator: AnswerSetValidator

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

    /** Returns a [Sequence] of all [DbSubmission]s connected to this [AbstractTask]. */
    override fun getSubmissions() = DbAnswerSet.filter {
        a -> a.task.id eq this@AbstractTask.taskId
    }.mapDistinct {
        it.submission
    }.asSequence()
}
package dev.dres.data.model.run

import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.run.TaskStatus
import kotlinx.dnq.util.findById

/**
 * An abstract [Run] implementation that can be used by different subtypes.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractTaskRun(task: Task): TaskRun {
    /** The internal [xdId] of this [AbstractEvaluation].
     *
     * Since this cannot change during the lifetime of an evaluation, it is kept in memory.
     */
    private val xdId = task.xdId

    /**
     * Accessor for the [Task] underpinning this [AbstractTaskRun]
     */
    protected val task: Task
        get() = Task.findById(this.xdId)

    /**
     * The [TaskId] of this [AbstractTaskRun].
     *
     * Since this cannot change during the lifetime of an evaluation, it is kept in memory.
     */
    override val id: TaskId = this.task.id

    /** Timestamp of when this [AbstractTaskRun] was started. */
    override var started: Long
        get() = this.task.started
        protected set(value) {
            this.task.started = value
        }

    /** Timestamp of when this [AbstractTaskRun] was ended. */
    override var ended: Long?
        get() = this.task.ended
        protected set(value) {
            this.task.ended = value
        }

    /** Reference to the [TaskTemplate] describing this [AbstractTaskRun]. */
    override val template: TaskTemplate
        get() = this.task.template

    @Volatile
    override var status: TaskStatus = TaskStatus.CREATED
        protected set

    fun prepare() {
        if (this.hasEnded) {
            throw IllegalStateException("Run has already ended.")
        }
        if (this.hasStarted) {
            throw IllegalStateException("Run has already been started.")
        }
        this.status = TaskStatus.PREPARING
    }

    /**
     * Starts this [AbstractTaskRun].
     */
    override fun start() {
        if (this.hasStarted) {
            throw IllegalStateException("Run has already been started.")
        }
        this.started = System.currentTimeMillis()
        this.status = TaskStatus.RUNNING
    }

    /**
     * Ends this [AbstractTaskRun].
     */
    override fun end() {
        if (!this.isRunning) {
            this.started = System.currentTimeMillis()
        }
        this.ended = System.currentTimeMillis()
        this.status = TaskStatus.ENDED
    }


    /**
     * Reactivates this [AbstractTaskRun].
     */
    override fun reactivate() {
        if (this.ended == null){
            throw IllegalStateException("Run has not yet ended.")
        }
        this.ended = null
        this.status = TaskStatus.RUNNING
    }
}
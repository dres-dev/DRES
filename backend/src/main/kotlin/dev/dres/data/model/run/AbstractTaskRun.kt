package dev.dres.data.model.run

import dev.dres.data.model.competition.task.TaskDescription
import dev.dres.data.model.run.interfaces.Run
import dev.dres.run.TaskRunStatus

/**
 * An abstract [Run] implementation that can be used by different subtypes.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractTaskRun(protected val task: Task): dev.dres.data.model.run.interfaces.TaskRun {
    /** The Id of this [AbstractTaskRun]. */
    override val id: TaskId
        get() = this.task.id

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

    /** Reference to the [TaskDescription] describing this [AbstractTaskRun]. */
    override val description: TaskDescription
        get() = this.task.description

    @Volatile
    override var status: TaskRunStatus = TaskRunStatus.CREATED
        protected set

    fun prepare() {
        if (this.hasEnded) {
            throw IllegalStateException("Run has already ended.")
        }
        if (this.hasStarted) {
            throw IllegalStateException("Run has already been started.")
        }
        this.status = TaskRunStatus.PREPARING
    }

    /**
     * Starts this [AbstractTaskRun].
     */
    override fun start() {
        if (this.hasStarted) {
            throw IllegalStateException("Run has already been started.")
        }
        this.started = System.currentTimeMillis()
        this.status = TaskRunStatus.RUNNING
    }

    /**
     * Ends this [AbstractTaskRun].
     */
    override fun end() {
        if (!this.isRunning) {
            this.started = System.currentTimeMillis()
        }
        this.ended = System.currentTimeMillis()
        this.status = TaskRunStatus.ENDED
    }

    override fun reactivate() {
        if (this.ended == null){
            throw IllegalStateException("Run has not yet ended.")
        }
        this.ended = null
        this.status = TaskRunStatus.RUNNING
    }
}
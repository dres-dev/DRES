package dev.dres.data.model.run


import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.run.interfaces.Task
import dev.dres.run.TaskRunStatus

/**
 * An abstract [Run] implementation that can be used by different subtypes.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractTaskRun: Task {

    /** Timestamp of when this [AbstractTaskRun] was started. */
    @Volatile
    override var started: Long? = null
        protected set

    /** Timestamp of when this [AbstractTaskRun] was ended. */
    @Volatile
    override var ended: Long? = null
        protected set

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
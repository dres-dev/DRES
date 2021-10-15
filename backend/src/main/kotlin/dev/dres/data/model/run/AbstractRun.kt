package dev.dres.data.model.run


import dev.dres.data.model.run.interfaces.Run

/**
 * An abstract [Run] implementation that can be used by different subtypes.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractRun: Run {

    /** Timestamp of when this [AbstractRun] was started. */
    @Volatile
    override var started: Long? = null
        protected set

    /** Timestamp of when this [AbstractRun] was ended. */
    @Volatile
    override var ended: Long? = null
        protected set

    /**
     * Starts this [AbstractRun].
     */
    override fun start() {
        if (this.hasStarted) {
            throw IllegalStateException("Run has already been started.")
        }
        this.started = System.currentTimeMillis()
    }

    /**
     * Ends this [AbstractRun].
     */
    override fun end() {
        if (!this.isRunning) {
            this.started = System.currentTimeMillis()
        }
        this.ended = System.currentTimeMillis()
    }

    override fun reactivate() {

        if (this.ended == null){
            throw IllegalStateException("Run has not yet ended.")
        }
        this.ended = null

    }
}
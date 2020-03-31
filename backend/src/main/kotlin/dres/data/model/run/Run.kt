package dres.data.model.run

/**
 * A [Run] that can be started and ended.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
interface Run {
    /** Timestamp of when this [Run] was started. */
    val started: Long?

    /** Timestamp of when this [Run] was ended. */
    val ended: Long?

    /** Boolean indicating whether this [Run] is still running. */
    val isRunning: Boolean
        get() = this.started != null && this.ended == null

    /** Boolean indicating whether this [Run] has ended. */
    val hasStarted: Boolean
        get() = this.started != null

    /** Boolean indicating whether this [Run] has ended. */
    val hasEnded: Boolean
        get() = this.ended != null

    /**
     * Starts this [Run]. Can only be invoked once.
     */
    fun start()

    /**
     * Ends this [Run]. Can only be invoked once.
     */
    fun end()
}






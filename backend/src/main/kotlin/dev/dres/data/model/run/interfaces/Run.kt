package dev.dres.data.model.run.interfaces

/**
 * A [Run] that can be started and ended and keeps track of the points in time, these events took place.
 *
 * @author Ralph Gasser
 * @version 1.0.0
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
     * Starts this [Run].
     *
     * @throws IllegalStateException If [Run] is already running or has ended.
     */
    fun start()

    /**
     * Ends this [Run].
     *
     * @throws IllegalStateException If [Run] has ended running.
     */
    fun end()

    /**
     * Reactivates an ended [Run].
     */
    fun reactivate()
}






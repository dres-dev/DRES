package dev.dres.utilities

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap
import java.util.HashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * A simple latch that tracks for all object it contains whether they are ready (true) or not (false).
 *
 * @author Ralph Gasser
 * @version 1.1.1
 */
class ReadyLatch<T> {

    /** Internal map that maps object of type [T] to its ready state. */
    private val map = Object2BooleanOpenHashMap<T>()

    /** Internal lock to mediate access to map. */
    private val lock = ReentrantReadWriteLock()

    private var timeout: Long? = null

    /**
     * Registers a new object [T] with this [ReadyLatch].
     *
     * @param o The object [T] to register.
     */
    fun register(o: T) = this.lock.write {
        this.map.put(o, false)
    }

    /**
     * Unregisters an object [T] with this [ReadyLatch].
     *
     * @param o The object [T] to unregister.
     */
    fun unregister(o: T) = this.lock.write {
        this.map.removeBoolean(o)
    }

    /**
     * Returns the state of this [ReadyLatch] as a [HashMap].
     *
     * @return Current state of this [ReadyLatch].
     */
    fun state() = this.lock.read {
        HashMap<T,Boolean>(this.map)
    }

    /**
     * Sets the ready state for the given object [T] to true.
     *
     * @param o The object [T] to set the ready state for.
     */
    fun setReady(o: T) = this.lock.write {
        require(this.map.containsKey(o)) { "Given key is not tracked by this ReadyLatch instance. "}
        this.map.put(o, true)
    }

    /**
     * Sets the ready state for the given object [T] to false.
     *
     * @param o The object [T] to set the ready state for.
     */
    fun setUnready(o: T) = this.lock.write {
        require(this.map.containsKey(o)) { "Given key is not tracked by this ReadyLatch instance. "}
        this.map.put(o, false)
    }

    /**
     * Resets this [ReadyLatch] and thus moves all registered objects to unready state.
     *
     * @param timeout specifies an optional timeout in seconds after which [allReadyOrTimedOut] is considered to be true in any case
     */
    fun reset(timeout: Long? = null) = this.lock.write {
        for (e in this.map.keys) {
            this.map[e] = false
        }
        this.timeout = timeout?.let { it + System.currentTimeMillis() }
    }

    /**
     * Clears this [ReadyLatch] thus removing all objects it tracks.
     */
    fun clear() = this.lock.write {
        this.map.clear()
    }

    /**
     * Returns true if and only if all objects registered with this [ReadyLatch] are in the ready state.
     */
    fun allReady() = this.lock.read {
        this.map.all { it.value }
    }

    /**
     * Equivalent to [allReady] in case no timeout was set in [reset]
     */
    fun allReadyOrTimedOut()
        = allReady() || (this.timeout ?: Long.MAX_VALUE) <= System.currentTimeMillis()
}
package dev.dres.utilities

import dev.dres.utilities.extensions.read
import dev.dres.utilities.extensions.write
import org.eclipse.collections.impl.map.mutable.primitive.ObjectBooleanHashMap
import java.util.HashMap
import java.util.concurrent.locks.StampedLock

/**
 * A simple latch that tracks for all object it contains whether they are ready (true) or not (false).
 *
 * @author Ralph Gasser
 * @version 1.1
 */
class ReadyLatch<T> {

    /** Internal map that maps object of type [T] to its ready state. */
    private val map = ObjectBooleanHashMap<T>()

    /** Internal lock to mediate access to map. */
    private val lock = StampedLock()

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
        this.map.remove(o)
    }

    /**
     * Returns the state of this [ReadyLatch] as a [HashMap].
     *
     * @return Current state of this [ReadyLatch].
     */
    fun state() = this.lock.read {
        val map = HashMap<T,Boolean>()
        this.map.forEachKeyValue { k, v -> map[k] = v}
        map
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
     */
    fun reset() = this.lock.write {
        this.map.updateValues { _, _ -> false }
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
        this.map.allSatisfy { it }
    }
}
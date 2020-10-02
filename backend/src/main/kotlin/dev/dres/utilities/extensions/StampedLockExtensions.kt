package dev.dres.utilities.extensions

import java.util.concurrent.locks.StampedLock

/**
 * Executes the given [action] under the read lock of this [StampedLock].
 *
 * @param action The action to execute. Must be side-effect free and
 * @return the return value of the action.
 */
inline fun <T> StampedLock.read(action: () -> T): T {
    val stamp = this.readLock()
    try {
        return action()
    } finally {
        this.unlock(stamp)
    }
}

/**
 * Tries to execute the given [action] under and optimistic read lock of this [StampedLock]. If the optimistic lock
 * fails or a lock was acquire while executing the action, then a fallback to an ordinary read lock is used.
 *
 * @param action The action to execute. Must be idempotent and side-effect free.
 * @return the return value of the action.
 */
inline fun <T> StampedLock.optimisticRead(action: () -> T): T {
    val stamp = this.tryOptimisticRead()
    if (stamp == 0L) {
        return this.read(action)
    }
    val ret = action()
    return if (this.validate(stamp)) {
        ret
    } else {
        this.read(action)
    }
}

/**
 * Executes the given [action] under the read lock of this [StampedLock].
 *
 * @param action The action to execute. Must be side-effect free.
 * @return the return value of the action.
 */
inline fun <T> StampedLock.write(action: () -> T): T {
    val stamp = this.writeLock()
    try {
        return action()
    } finally {
        this.unlock(stamp)
    }
}

/**
 * Executes the given [action] under the read lock of this [StampedLock].
 *
 * @param action The action to execute. Must be side-effect free.
 * @return the return value of the action.
 */
fun StampedLock.convertWriteLock(stamp: Long): Long {
    var new = this.tryConvertToWriteLock(stamp)
    while (new == 0L) {
        new = this.tryConvertToWriteLock(stamp)
        Thread.onSpinWait()
    }
    return new
}
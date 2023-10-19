package dev.dres.utilities

import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * A [Future] implementation that encapsulates an completed value.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class CompletedFuture<T>(private val value: T): Future<T> {
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
    override fun isCancelled(): Boolean = false
    override fun isDone(): Boolean =true
    override fun get(): T = this.value
    override fun get(timeout: Long, unit: TimeUnit): T = this.get()
}
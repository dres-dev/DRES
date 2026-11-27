package dev.dres.run

import java.util.ArrayDeque
import java.util.concurrent.ForkJoinPool

/**
 * Sends websocket text frames in FIFO order without blocking the event-dispatch thread.
 * A full queue closes the slow connection instead of silently dropping an event.
 */
internal class BoundedSerialMessageSender(
    private val maxPendingMessages: Int,
    private val sendAsync: (String, () -> Unit, (Throwable) -> Unit) -> Unit,
    private val closeSession: (Int, String) -> Unit,
    private val onClosed: () -> Unit = {}
) {
    private val lock = Any()
    private val pending = ArrayDeque<String>()
    private var sending = false
    private var closed = false

    init {
        require(maxPendingMessages > 0) { "maxPendingMessages must be positive" }
    }

    fun cancel() {
        synchronized(lock) {
            closed = true
            pending.clear()
            sending = false
        }
    }

    fun send(message: String) {
        var start = false
        var overflow = false
        synchronized(lock) {
            if (closed) return
            if (pending.size >= maxPendingMessages) {
                closed = true
                pending.clear()
                sending = false
                overflow = true
            } else {
                pending.addLast(message)
                if (!sending) {
                    sending = true
                    start = true
                }
            }
        }
        if (overflow) {
            close(OVERLOAD_CLOSE_CODE, "Outbound websocket queue is full")
            return
        }
        if (start) dispatchHead()
    }

    private fun dispatchHead() {
        val message = synchronized(lock) { pending.peekFirst() } ?: return
        try {
            sendAsync(message, ::sent, ::failed)
        } catch (error: Throwable) {
            failed(error)
        }
    }

    private fun sent() {
        val dispatchNext = synchronized(lock) {
            if (closed) return
            pending.pollFirst()
            if (pending.isEmpty()) {
                sending = false
                false
            } else {
                true
            }
        }
        if (dispatchNext) {
            /* Avoid recursive dispatch if a transport completes callbacks inline. */
            ForkJoinPool.commonPool().execute(::dispatchHead)
        }
    }

    private fun failed(error: Throwable) {
        val shouldClose = synchronized(lock) {
            if (closed) false else {
                closed = true
                pending.clear()
                sending = false
                true
            }
        }
        if (shouldClose) {
            close(FAILURE_CLOSE_CODE, "Websocket send failed: ${error.message ?: "unknown error"}")
        }
    }

    private fun close(code: Int, reason: String) {
        try {
            closeSession(code, reason.take(MAX_CLOSE_REASON_LENGTH))
        } catch (_: Throwable) {
            /* The transport may already be closing; still release our sender state. */
        } finally {
            onClosed()
        }
    }

    companion object {
        const val OVERLOAD_CLOSE_CODE = 1013
        const val FAILURE_CLOSE_CODE = 1011
        private const val MAX_CLOSE_REASON_LENGTH = 120
    }
}

package dev.dres.run

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoundedSerialMessageSenderTest {
    @Test
    fun `sends messages one at a time in fifo order`() {
        val inFlight = java.util.concurrent.LinkedBlockingQueue<Pair<String, () -> Unit>>()
        val sent = java.util.concurrent.CopyOnWriteArrayList<String>()
        val sender = BoundedSerialMessageSender(
            maxPendingMessages = 3,
            sendAsync = { message, success, _ -> inFlight.add(message to { sent.add(message); success() }) },
            closeSession = { _, _ -> error("Unexpected close") }
        )

        sender.send("first")
        sender.send("second")
        sender.send("third")

        assertEquals(listOf("first"), inFlight.map { it.first })
        inFlight.remove().second()
        val second = inFlight.poll(1, java.util.concurrent.TimeUnit.SECONDS)
        assertEquals("second", second.first)
        assertEquals(listOf("first"), sent)
        second.second()
        val third = inFlight.poll(1, java.util.concurrent.TimeUnit.SECONDS)
        assertEquals("third", third.first)
        assertEquals(listOf("first", "second"), sent)
        third.second()
        assertEquals(listOf("first", "second", "third"), sent)
        assertTrue(inFlight.isEmpty())
    }

    @Test
    fun `closes slow client on bounded queue overflow without dropping silently`() {
        var closeCode: Int? = null
        var closedCallbackCount = 0
        var success: (() -> Unit)? = null
        val sender = BoundedSerialMessageSender(
            maxPendingMessages = 1,
            sendAsync = { _, onSuccess, _ -> success = onSuccess },
            closeSession = { code, _ -> closeCode = code },
            onClosed = { closedCallbackCount++ }
        )

        sender.send("in-flight")
        sender.send("queued")
        sender.send("overflow")
        sender.send("ignored")

        assertEquals(BoundedSerialMessageSender.OVERLOAD_CLOSE_CODE, closeCode)
        assertEquals(1, closedCallbackCount)
        success?.invoke()
        assertEquals(1, closedCallbackCount)
    }

    @Test
    fun `closes connection and clears queue after send failure`() {
        var closeCode: Int? = null
        val sender = BoundedSerialMessageSender(
            maxPendingMessages = 2,
            sendAsync = { _, _, failure -> failure(IllegalStateException("broken")) },
            closeSession = { code, _ -> closeCode = code }
        )

        sender.send("first")
        sender.send("second")

        assertEquals(BoundedSerialMessageSender.FAILURE_CLOSE_CODE, closeCode)
        sender.send("after-close")
        assertFalse(closeCode == BoundedSerialMessageSender.OVERLOAD_CLOSE_CODE)
    }
}

package dres.run.eventstream

import dev.dres.DRES
import dev.dres.data.model.config.Config
import dev.dres.run.eventstream.EventStreamProcessor
import dev.dres.run.eventstream.StreamEvent
import dev.dres.run.eventstream.StreamEventHandler
import dev.dres.run.eventstream.TaskEndEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * The processor thread is a daemon, so a shutdown that only flips a flag can drop events that are
 * already queued: the JVM exits, or the file is closed before they are written. `stop()` now drains the
 * queue and joins the thread, and these tests pin both halves.
 *
 * `EventStreamProcessor` is a singleton without a way to unregister a handler, so this class registers
 * exactly one handler that forwards to whichever consumer the running test installs.
 */
class EventStreamProcessorShutdownTest {

    companion object {
        /** Events the currently running test wants to observe. */
        private val delivered = ConcurrentLinkedQueue<String>()

        /** Invoked for every delivered event so a test can block the processor thread. */
        @Volatile
        private var onEvent: (StreamEvent) -> Unit = {}

        /** Set for exactly one event to prove a throwing handler does not kill the thread. */
        @Volatile
        private var failOnce = false

        init {
            EventStreamProcessor.register(object : StreamEventHandler {
                override fun handleStreamEvent(event: StreamEvent) {
                    onEvent(event)
                    if (failOnce) {
                        failOnce = false
                        throw IllegalStateException("handler failure")
                    }
                    delivered.add(event::class.simpleName ?: "?")
                }
            })
        }
    }

    @BeforeEach
    fun reset() {
        configure()
        delivered.clear()
        onEvent = {}
        failOnce = false
    }

    private fun configure() {
        DRES.CONFIG = Config(eventsLocation = Files.createTempDirectory("dres-events-shutdown-"))
    }

    /** A slow handler widens the window in which queued events would have been lost. */
    @Test
    fun `stop waits for queued events to be handled`() {
        val started = CountDownLatch(1)
        onEvent = {
            started.countDown()
            Thread.sleep(10)
        }
        EventStreamProcessor.init()

        val total = 20
        repeat(total) { EventStreamProcessor.event(TaskEndEvent("run-$it", "task-$it")) }
        assertTrue(started.await(5, TimeUnit.SECONDS), "The processor thread never picked up an event")

        EventStreamProcessor.stop()

        assertEquals(
            total,
            delivered.size,
            "Every queued event must be handled before stop() returns."
        )
    }

    /** `stop()` is called from a shutdown hook and from tests, so a second call must not block or throw. */
    @Test
    fun `stop is idempotent`() {
        EventStreamProcessor.init()
        EventStreamProcessor.event(TaskEndEvent("run", "task"))

        EventStreamProcessor.stop()
        EventStreamProcessor.stop()

        assertEquals(1, delivered.size)
    }

    /** A handler that throws is logged; it must not take the processor, and every later event, down. */
    @Test
    fun `a failing handler does not stop the processor`() {
        val attempts = AtomicInteger()
        onEvent = {
            if (attempts.getAndIncrement() == 0) {
                failOnce = true
            }
        }
        EventStreamProcessor.init()

        EventStreamProcessor.event(TaskEndEvent("run", "first"))
        EventStreamProcessor.event(TaskEndEvent("run", "second"))
        EventStreamProcessor.stop()

        assertTrue(attempts.get() >= 2, "The processor stopped after the first handler failure")
        assertTrue(delivered.isNotEmpty(), "An event after the failure must still be delivered")
    }
}

package dev.dres.run.eventstream

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.dres.DRES
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.thread
import kotlin.concurrent.write

object EventStreamProcessor {

    private const val flushInterval = 30_000

    /**
     * Whether the processor thread should keep draining the queue.
     *
     * Set by [init]/[stop] and read by the processor thread, so it must be `@Volatile`: a cached read
     * could keep the thread alive after [stop] or, worse, let it exit before queued events were written.
     */
    @Volatile
    private var active = false
    private var flushTimer = 0L

    private lateinit var processorThread: Thread
    private val mapper = jacksonObjectMapper()
    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    private val eventQueue = LinkedBlockingQueue<QueuedEvent>()
    private val eventHandlers = mutableListOf<StreamEventHandler>()
    private val handlerLock = ReentrantReadWriteLock()

    /*
     * The event file is opened in `init()` rather than in the object initializer. The initializer used to
     * read `DRES.CONFIG.eventsLocation`, so merely touching this object threw
     * "lateinit property CONFIG has not been initialized".
     */
    private data class QueuedEvent(val event: StreamEvent, val enqueuedAt: Long)

    fun event(event: StreamEvent) = eventQueue.add(QueuedEvent(event, System.nanoTime()))
    fun register(vararg handler: StreamEventHandler) = handlerLock.write { eventHandlers.addAll(handler) }

    fun init() {
        if (active) {
            return
        }

        active = true

        /*
         * The sink is local to the thread, so a stop/init cycle opens a fresh file instead of appending to
         * the writer the previous thread already closed (writes to a closed PrintWriter are swallowed).
         */
        val sink = PrintWriter(
            DRES.CONFIG.eventsLocation.resolve("${System.currentTimeMillis()}.txt").toFile()
                .also { it.parentFile.mkdirs() })

        processorThread = thread(name = "EventStreamProcessorThread", isDaemon = true, start = true) {

            /*
             * Drain before exiting: `stop()` sets `active = false`, but events that were queued before the
             * shutdown signal are still owed to the event file. The loop used to test `active` first and
             * drop them.
             */
            while (active || eventQueue.isNotEmpty()) {
                try {

                    val queued = eventQueue.poll(1, TimeUnit.SECONDS) ?: continue
                    val event = queued.event
                    val queueDelayMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - queued.enqueuedAt)
                    if (queueDelayMs >= 100) {
                        LOGGER.warn("Event queue delay was {}ms for {} (queue size: {})", queueDelayMs, event::class.simpleName, eventQueue.size)
                    }

                    handlerLock.read {
                        for (handler in eventHandlers) {
                            val startedAt = System.nanoTime()
                            try {
                                handler.handleStreamEvent(event)
                            } catch (t: Throwable) {
                                LOGGER.error(
                                    "Uncaught exception while handling event $event in ${handler.javaClass.simpleName}",
                                    t
                                )
                            } finally {
                                val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
                                if (durationMs >= 100) {
                                    LOGGER.warn("Event handler {} took {}ms for {}", handler.javaClass.simpleName, durationMs, event::class.simpleName)
                                }
                            }
                        }
                    }

                    try {
                        sink.println(
                            mapper.writeValueAsString(event)
                        )
                    } catch (t: Throwable) {
                        LOGGER.error("Error while storing event $event", t)
                    }


                } catch (t: Throwable) {
                    LOGGER.error("Uncaught exception in EventStreamProcessor", t)
                }

                if (flushTimer < System.currentTimeMillis()) {
                    sink.flush()
                    flushTimer = System.currentTimeMillis() + flushInterval
                }
            }

            sink.flush()
            sink.close()

        }

    }

    /**
     * Stops accepting work and waits for the processor thread to write what is already queued.
     *
     * Joining matters at shutdown: the thread is a daemon, so without it the JVM may exit while events
     * are still only in the queue, and the flush and close at the end of the loop never run.
     */
    fun stop() {
        active = false
        if (this::processorThread.isInitialized) {
            try {
                processorThread.join(TimeUnit.SECONDS.toMillis(10))
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                LOGGER.warn("Interrupted while waiting for the event stream processor to shut down.", e)
            }
            if (processorThread.isAlive) {
                LOGGER.warn("Event stream processor did not shut down within 10s; up to {} queued events may be lost.", eventQueue.size)
            }
        }
    }

}

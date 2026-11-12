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

    private var active = false
    private var flushTimer = 0L

    private lateinit var processorThread: Thread
    private val mapper = jacksonObjectMapper()
    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    private val eventQueue = LinkedBlockingQueue<QueuedEvent>()
    private val eventHandlers = mutableListOf<StreamEventHandler>()
    private val handlerLock = ReentrantReadWriteLock()
    private val eventSink = PrintWriter(
        DRES.CONFIG.eventsLocation.resolve("${System.currentTimeMillis()}.txt").toFile()
            .also { it.parentFile.mkdirs() })


    private data class QueuedEvent(val event: StreamEvent, val enqueuedAt: Long)

    fun event(event: StreamEvent) = eventQueue.add(QueuedEvent(event, System.nanoTime()))
    fun register(vararg handler: StreamEventHandler) = handlerLock.write { eventHandlers.addAll(handler) }

    fun init() {
        if (active) {
            return
        }

        active = true

        processorThread = thread(name = "EventStreamProcessorThread", isDaemon = true, start = true) {

            while (active) {
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
                        eventSink.println(
                            mapper.writeValueAsString(event)
                        )
                    } catch (t: Throwable) {
                        LOGGER.error("Error while storing event $event", t)
                    }


                } catch (t: Throwable) {
                    LOGGER.error("Uncaught exception in EventStreamProcessor", t)
                }

                if (flushTimer < System.currentTimeMillis()) {
                    eventSink.flush()
                    flushTimer = System.currentTimeMillis() + flushInterval
                }
            }

            eventSink.flush()
            eventSink.close()

        }

    }

    fun stop() {
        active = false
    }

}

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

    private val eventQueue = LinkedBlockingQueue<StreamEvent>()
    private val eventHandlers = mutableListOf<StreamEventHandler>()
    private val handlerLock = ReentrantReadWriteLock()
    private val eventSink = PrintWriter(
        DRES.CONFIG.eventsLocation.resolve("${System.currentTimeMillis()}.txt").toFile()
            .also { it.parentFile.mkdirs() })


    fun event(event: StreamEvent) = eventQueue.add(event)
    fun register(vararg handler: StreamEventHandler) = handlerLock.write { eventHandlers.addAll(handler) }

    fun init() {
        if (active) {
            return
        }

        active = true

        processorThread = thread(name = "EventStreamProcessorThread", isDaemon = true, start = true) {

            while (active) {
                try {

                    val event = eventQueue.poll(1, TimeUnit.SECONDS) ?: continue

                    handlerLock.read {
                        for (handler in eventHandlers) {
                            try {
                                handler.handleStreamEvent(event)
                            } catch (t: Throwable) {
                                LOGGER.error(
                                    "Uncaught exception while handling event $event in ${handler.javaClass.simpleName}",
                                    t
                                )
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

package dev.dres.run.eventstream

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.dres.utilities.extensions.read
import dev.dres.utilities.extensions.write
import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintWriter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.StampedLock

object EventStreamProcessor {

    private const val flushInterval = 30_000

    private var active = false
    private var flushTimer = 0L

    private lateinit var processorThread: Thread
    private val mapper = jacksonObjectMapper()
    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    private val eventQueue = ConcurrentLinkedQueue<StreamEvent>()
    private val eventHandlers = mutableListOf<StreamEventHandler>()
    private val handlerLock = StampedLock()
    private val eventSink = PrintWriter(File("events/${System.currentTimeMillis()}.txt").also { it.parentFile.mkdirs() })

    private val eventBuffer = mutableListOf<StreamEvent>()
    private val eventBufferRetentionTime = 60_000 //TODO make configurable
    private val eventBufferLock = StampedLock()

    val recentEvents: List<StreamEvent>
    get() = eventBufferLock.read {eventBuffer}

    fun event(event: StreamEvent) = eventQueue.add(event)
    fun register(vararg handler: StreamEventHandler) = handlerLock.write { eventHandlers.addAll(handler) }

    fun init() {
        if (active) {
            return
        }

        active = true

        processorThread = Thread( {

            while (active) {
                try {

                    while (eventQueue.isNotEmpty()) {
                        val event = eventQueue.poll() ?: break

                        handlerLock.read {
                            for (handler in eventHandlers) {
                                try {
                                    handler.handle(event)
                                } catch (t: Throwable) {
                                    LOGGER.error("Uncaught exception while handling event $event in ${handler.javaClass.simpleName}", t)
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

                        eventBufferLock.write { eventBuffer.add(event) }
                    }

                    val removeThreshold = System.currentTimeMillis() - eventBufferRetentionTime
                    eventBufferLock.write { eventBuffer.removeIf { it.timeStamp < removeThreshold } }


                } catch (t : Throwable) {
                    LOGGER.error("Uncaught exception in EventStreamProcessor", t)
                } finally {
                    Thread.sleep(10)
                }

                if (flushTimer < System.currentTimeMillis()){
                    eventSink.flush()
                    flushTimer = System.currentTimeMillis() + flushInterval
                }

            }

            eventSink.flush()
            eventSink.close()

        }, "EventStreamProcessorThread")

        processorThread.isDaemon = true
        processorThread.start()

    }

    fun stop(){
        active = false
    }

}
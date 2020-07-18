package dres.run.eventstream

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dres.utilities.extensions.read
import dres.utilities.extensions.write
import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintWriter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.StampedLock

object EventStreamProcessor {

    private var active = false

    private lateinit var processorThread: Thread
    private val mapper = ObjectMapper().apply { registerModule(KotlinModule()) }
    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    private val eventQueue = ConcurrentLinkedQueue<StreamEvent>()
    private val eventHandlers = mutableListOf<StreamEventHandler>()
    private val handlerLock = StampedLock()
    private val eventSink = PrintWriter(File("events/${System.currentTimeMillis()}.txt").also { it.parentFile.mkdirs() })

    fun event(event: StreamEvent) = eventQueue.add(event)
    fun register(handler: StreamEventHandler) = handlerLock.write { eventHandlers.add(handler) }

    fun init() {
        if (active) {
            return
        }

        active = true

        processorThread = Thread {

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

                    }

                } catch (t : Throwable) {
                    LOGGER.error("Uncaught exception in EventStreamProcessor", t)
                } finally {
                    Thread.sleep(10)
                }
            }

            eventSink.flush()
            eventSink.close()

        }

        processorThread.start()

    }

    fun stop(){
        active = false
    }

}
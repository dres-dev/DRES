package dev.dres.run

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.dres.api.rest.types.ViewerInfo
import dev.dres.api.rest.types.evaluation.websocket.ClientMessage
import dev.dres.api.rest.types.evaluation.websocket.ClientMessageType
import dev.dres.api.rest.types.evaluation.websocket.ServerMessage
import dev.dres.api.rest.types.evaluation.websocket.ServerMessageType
import dev.dres.data.model.run.*
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.run.eventstream.*
import dev.dres.run.validation.interfaces.JudgementValidator
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsContext
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * The execution environment for [RunManager]s.
 *
 * @author Ralph Gasser
 * @version 1.4.0
 */
object RunExecutor : StreamEventHandler {

    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val mapper = jacksonObjectMapper()

    /** Thread Pool Executor which is used to execute the [RunManager]s. */
    private val executor = Executors.newCachedThreadPool()

    /** List of [RunManager] executed by this [RunExecutor]. */
    private val runManagers = HashMap<EvaluationId, RunManager>()

    /** List of [JudgementValidator]s registered with this [RunExecutor]. */
    private val judgementValidators = LinkedList<JudgementValidator>()

    /** WebSocket sessions currently connected, keyed by session ID. */
    private val connectedClients = ConcurrentHashMap<String, WsContext>()

    /** Session IDs currently observing each evaluation. */
    private val observingClients = HashMap<EvaluationId, MutableSet<String>>()

    /** Lock for WebSocket client data structures. */
    private val clientLock = ReentrantReadWriteLock()

    /** Lock for accessing and changing all data structures related to [RunManager]s. */
    private val runManagerLock = ReentrantReadWriteLock()

    /** Internal array of [Future]s for cleaning after [RunManager]s. */
    private val results = HashMap<Future<*>, EvaluationId>()

    /**
     * Initializes the [RunExecutor] singleton.
     *
     * @param store The [TransientEntityStore] instance used to access persistent data.
     */
    fun init(store: TransientEntityStore) {
        store.transactional {
            DbEvaluation.filter { (it.ended eq null) }.asSequence().forEach { evaluation ->
                try {
                    this.schedule(evaluation.toRunManager(store))
                } catch (e: RuntimeException) {
                    when (e) {
                        is IllegalStateException,
                        is IllegalArgumentException -> {
                            logger.error("Could not re-schedule previous run: ${e.message}")
                            evaluation.ended = System.currentTimeMillis()
                        }
                        else -> {
                            logger.error("Fatal error during re-scheduling of previous run (${evaluation.evaluationId}): ${e.message}")
                            throw e
                        }
                    }
                }
            }
        }
    }

    /**
     * Schedules a new [RunManager] with this [RunExecutor].
     *
     * @param manager [RunManager] to execute.
     */
    fun schedule(manager: RunManager) = this.runManagerLock.write {
        if (this.runManagers.containsKey(manager.id)) {
            throw IllegalArgumentException("This RunExecutor already runs a RunManager with the given ID ${manager.id}. The same RunManager cannot be executed twice!")
        }
        this.runManagers[manager.id] = manager
        this.observingClients[manager.id] = HashSet()
        this.results[this.executor.submit(manager)] = manager.id
    }

    /** A thread that cleans after [RunManager]s have finished. */
    private val cleanerThread = Thread {
        while (!this@RunExecutor.executor.isShutdown) {
            this@RunExecutor.runManagerLock.read {
                this@RunExecutor.results.entries.removeIf { entry ->
                    val k = entry.key
                    val v = entry.value
                    if (k.isDone || k.isCancelled) {
                        logger.info("RunManager $v (done = ${k.isDone}, cancelled = ${k.isCancelled}) will be removed!")
                        this@RunExecutor.runManagerLock.write {
                            this@RunExecutor.runManagers.remove(v)
                            this@RunExecutor.observingClients.remove(v)
                        }
                        true
                    } else {
                        false
                    }
                }
            }
            Thread.sleep(500)
        }
    }

    init {
        this.cleanerThread.priority = Thread.MIN_PRIORITY
        this.cleanerThread.isDaemon = true
        this.cleanerThread.name = "run-manager-cleaner"
        this.cleanerThread.start()
    }

    /**
     * Callback for registering this [RunExecutor] as Javalin's WebSocket handler.
     *
     * @param ws The [WsConfig] to configure.
     */
    fun accept(ws: WsConfig) {
        ws.onConnect { ctx ->
            this.clientLock.write {
                this.connectedClients[ctx.sessionId()] = ctx
            }
            logger.debug("WebSocket client connected: ${ctx.sessionId()}")
        }

        ws.onClose { ctx ->
            this.clientLock.write {
                this.connectedClients.remove(ctx.sessionId())
                this.runManagerLock.read {
                    this.observingClients.values.forEach { it.remove(ctx.sessionId()) }
                }
            }
            logger.debug("WebSocket client disconnected: ${ctx.sessionId()}")
        }

        ws.onMessage { ctx ->
            val message = try {
                ctx.messageAsClass<ClientMessage>()
            } catch (e: Exception) {
                logger.warn("Cannot parse WebSocket message: ${e.localizedMessage}")
                return@onMessage
            }
            logger.debug("Received WebSocket message: $message from ${ctx.sessionId()}")
            this.runManagerLock.read {
                if (this.runManagers.containsKey(message.evaluationId)) {
                    when (message.type) {
                        ClientMessageType.ACK -> {}
                        ClientMessageType.REGISTER -> this.clientLock.write {
                            this.observingClients[message.evaluationId]?.add(ctx.sessionId())
                        }
                        ClientMessageType.UNREGISTER -> this.clientLock.write {
                            this.observingClients[message.evaluationId]?.remove(ctx.sessionId())
                        }
                        ClientMessageType.PING -> ctx.send(
                            mapper.writeValueAsString(ServerMessage(message.evaluationId, ServerMessageType.PING))
                        )
                    }
                }
            }
        }

        ws.onError { ctx ->
            logger.error("WebSocket error for session ${ctx.sessionId()}: ${ctx.error()?.message}")
            this.clientLock.write {
                this.connectedClients.remove(ctx.sessionId())
            }
        }
    }

    /**
     * Broadcasts a [ServerMessage] to all clients observing the specified evaluation.
     *
     * @param message The [ServerMessage] to broadcast.
     */
    fun broadcastWsMessage(message: ServerMessage) {
        val json = try {
            mapper.writeValueAsString(message)
        } catch (e: Exception) {
            logger.error("Failed to serialize ServerMessage: ${e.message}")
            return
        }
        val observers = this.clientLock.read {
            this.runManagerLock.read {
                this.observingClients[message.evaluationId]?.toSet() ?: emptySet()
            }
        }
        observers.forEach { sessionId ->
            try {
                this.connectedClients[sessionId]?.send(json)
            } catch (e: Exception) {
                logger.warn("Failed to send WebSocket message to session $sessionId: ${e.message}")
            }
        }
    }

    /**
     * Maps a [StreamEvent] to the [ServerMessage] that should be broadcast, or null
     * if the event type requires no WebSocket notification.
     */
    internal fun eventToMessage(event: StreamEvent): ServerMessage? = when (event) {
        is RunStartEvent   -> ServerMessage(event.runId, ServerMessageType.COMPETITION_START)
        is RunEndEvent     -> ServerMessage(event.runId, ServerMessageType.COMPETITION_END)
        is TaskStartEvent  -> ServerMessage(event.runId, ServerMessageType.TASK_START, event.taskId)
        is TaskEndEvent    -> ServerMessage(event.runId, ServerMessageType.TASK_END,   event.taskId)
        is ScoreUpdateEvent -> ServerMessage(event.runId, ServerMessageType.COMPETITION_UPDATE)
        is SubmissionEvent  -> ServerMessage(event.runId, ServerMessageType.TASK_UPDATED)
        else -> null
    }

    /**
     * Handles [StreamEvent]s from the [EventStreamProcessor] by broadcasting
     * the appropriate [ServerMessage] to all registered observers.
     */
    override fun handleStreamEvent(event: StreamEvent) {
        eventToMessage(event)?.let { broadcastWsMessage(it) }
    }

    /**
     * Lists all [RunManager]s currently executed by this [RunExecutor].
     */
    fun managers(): List<RunManager> = this.runManagerLock.read {
        return this.runManagers.values.toList()
    }

    /**
     * Returns the [RunManager] for the given ID if such a [RunManager] exists.
     */
    fun managerForId(evaluationId: EvaluationId): RunManager? = this.runManagerLock.read {
        return this.runManagers[evaluationId]
    }

    /**
     * Stops all runs.
     */
    fun stop() {
        this.executor.shutdownNow()
    }
}

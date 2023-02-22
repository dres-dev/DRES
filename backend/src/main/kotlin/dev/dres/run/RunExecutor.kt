package dev.dres.run

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.WebSocketConnection
import dev.dres.api.rest.types.evaluation.websocket.ClientMessage
import dev.dres.api.rest.types.evaluation.websocket.ClientMessageType
import dev.dres.api.rest.types.evaluation.websocket.ServerMessage
import dev.dres.api.rest.types.evaluation.websocket.ServerMessageType
import dev.dres.data.model.run.*
import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.run.interfaces.EvaluationRun
import dev.dres.run.validation.interfaces.JudgementValidator
import dev.dres.utilities.extensions.read
import dev.dres.utilities.extensions.write
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsContext
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.locks.StampedLock
import java.util.function.Consumer

/**
 * The execution environment for [RunManager]s
 *
 * @author Ralph Gasser
 * @version 1.2.0
 */
object RunExecutor : Consumer<WsConfig> {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    /** Thread Pool Executor which is used to execute the [RunManager]s. */
    private val executor = Executors.newCachedThreadPool()

    /** List of [RunManager] executed by this [RunExecutor]. */
    private val runManagers = HashMap<EvaluationId,RunManager>()

    /** List of [JudgementValidator]s registered with this [RunExecutor]. */
    private val judgementValidators = LinkedList<JudgementValidator>()

    /** List of [WsContext] that are currently connected. */
    private val connectedClients = HashMap<String,WebSocketConnection>()

    /** List of session IDs that are currently observing a competition. */
    private val observingClients = HashMap<EvaluationId, MutableSet<WebSocketConnection>>()

    /** Lock for accessing and changing all data structures related to WebSocket clients. */
    private val clientLock = StampedLock()

    /** Lock for accessing and changing all data structures related to [RunManager]s. */
    private val runManagerLock = StampedLock()

    /** Internal array of [Future]s for cleaning after [RunManager]s. See [RunExecutor.cleanerThread]*/
    private val results = HashMap<Future<*>, EvaluationId>()

    /**
     * Initializes this [RunExecutor].
     *
     * @param store The shared [TransientEntityStore].
     */
    fun init(store: TransientEntityStore) {
        store.transactional {
            DbEvaluation.filter { (it.started) ne null and (it.ended eq null) }.asSequence().forEach {e ->

                /* Force-end tasks that are still running. */
                e.tasks.filter { t -> (t.ended eq null) }.asSequence().forEach { t ->
                    t.ended = System.currentTimeMillis()
                }

                this.schedule(when (e.type) {
                    DbEvaluationType.INTERACTIVE_SYNCHRONOUS -> InteractiveSynchronousEvaluation(e)
                    DbEvaluationType.INTERACTIVE_ASYNCHRONOUS -> InteractiveAsynchronousEvaluation(e, emptyMap()) /* TODO: Team map. */
                    DbEvaluationType.NON_INTERACTIVE -> TODO()
                    else -> throw IllegalStateException("Unsupported evaluation type ${e.type}.")
                }, store)
            }
        }
    }

    /**
     * Schedules a new [EvaluationRun] with this [RunExecutor].
     *
     * @param evaluation [EvaluationRun] to execute.
     * @param store [TransientEntityStore] instanced used to interact with database.
     */
    fun schedule(evaluation: EvaluationRun, store: TransientEntityStore) {
        val run = when(evaluation) {
            is InteractiveSynchronousEvaluation -> InteractiveSynchronousRunManager(evaluation, store)
            is InteractiveAsynchronousEvaluation -> InteractiveAsynchronousRunManager(evaluation, store)
            is NonInteractiveEvaluation -> NonInteractiveRunManager(evaluation, store)
            else -> throw NotImplementedError("No matching run manager found for $evaluation")
        }
        this.schedule(run)
    }

    /**
     * Schedules a new [RunManager] with this [RunExecutor].
     *
     * @param manager [RunManager] to execute.
     */
    private fun schedule(manager: RunManager) = this.runManagerLock.write {
        if (this.runManagers.containsKey(manager.id)) {
            throw IllegalArgumentException("This RunExecutor already runs a RunManager with the given ID ${manager.id}. The same RunManager cannot be executed twice!")
        }

        /* Register [RunManager] with AccessManager. */
        AccessManager.registerRunManager(manager)

        /* Setup all the required data structures. */
        this.runManagers[manager.id] = manager
        this.observingClients[manager.id] = HashSet()
        this.results[this.executor.submit(manager)] = manager.id /* Register future for cleanup thread. */
    }

    /** A thread that cleans after [RunManager] have finished. */
    private val cleanerThread = Thread {
        while (!this@RunExecutor.executor.isShutdown) {
            var stamp = this@RunExecutor.runManagerLock.readLock()
            try {
                this@RunExecutor.results.entries.removeIf { entry ->
                    val k = entry.key
                    val v = entry.value
                    if (k.isDone || k.isCancelled) {
                        logger.info("RunManager $v (done = ${k.isDone}, cancelled = ${k.isCancelled}) will be removed!")
                        stamp = this@RunExecutor.runManagerLock.tryConvertToWriteLock(stamp)
                        if (stamp > -1L) {
                            /* Deregister the RunManager. */
                            AccessManager.deregisterRunManager(this@RunExecutor.runManagers[v]!!)

                            /* Cleanup. */
                            this@RunExecutor.runManagers.remove(v)
                            this@RunExecutor.observingClients.remove(v)
                        }
                        true
                    } else {
                        false
                    }
                }
            } finally {
                this@RunExecutor.runManagerLock.unlock(stamp)
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
     * Callback for when registering this [RunExecutor] as handler for Javalin's WebSocket.
     *
     * @param t The [WsConfig] of the WebSocket endpoint.
     */
    override fun accept(t: WsConfig) {
        t.onConnect {
            /* Add WSContext to set of connected clients. */
            this@RunExecutor.clientLock.write {
                val connection = WebSocketConnection(it)
                this.connectedClients[connection.httpSessionId] = connection
            }
        }
        t.onClose {
            val session = WebSocketConnection(it)
            this@RunExecutor.clientLock.write {
                val connection = WebSocketConnection(it)
                this.connectedClients.remove(connection.httpSessionId)
                this.runManagerLock.read {
                    for (m in this.runManagers) {
                        if (this.observingClients[m.key]?.contains(connection) == true) {
                            this.observingClients[m.key]?.remove(connection)
                            m.value.wsMessageReceived(session, ClientMessage(m.key, ClientMessageType.UNREGISTER)) /* Send implicit unregister message associated with a disconnect. */
                        }
                    }
                }
            }
        }
        t.onMessage {
            val message = try {
                it.messageAsClass<ClientMessage>()
            } catch (e: Exception) {
                logger.warn("Cannot parse WebSocket message: ${e.localizedMessage}")
                return@onMessage
            }
            val session = WebSocketConnection(it)
            logger.debug("Received WebSocket message: $message from ${it.session.policy}")
            this.runManagerLock.read {
                if (this.runManagers.containsKey(message.evaluationId)) {
                    when (message.type) {
                        ClientMessageType.ACK -> {}
                        ClientMessageType.REGISTER -> this@RunExecutor.clientLock.write { this.observingClients[message.evaluationId]?.add(WebSocketConnection(it)) }
                        ClientMessageType.UNREGISTER -> this@RunExecutor.clientLock.write { this.observingClients[message.evaluationId]?.remove(WebSocketConnection(it)) }
                        ClientMessageType.PING -> it.send(ServerMessage(message.evaluationId, null, ServerMessageType.PING))
                    }
                    this.runManagers[message.evaluationId]!!.wsMessageReceived(session, message) /* Forward message to RunManager. */
                }
            }
        }
    }

    /**
     * Lists all [RunManager]s currently executed by this [RunExecutor]
     *
     * @return Immutable list of all [RunManager]s currently executed.
     */
    fun managers(): List<RunManager> = this.runManagerLock.read {
        return this.runManagers.values.toList()
    }

    /**
     * Returns the [RunManager] for the given ID if such a [RunManager] exists.
     *
     * @param evaluationId The ID for which to return the [RunManager].
     * @return Optional [RunManager].
     */
    fun managerForId(evaluationId: EvaluationId): RunManager? = this.runManagerLock.read {
        return this.runManagers[evaluationId]
    }

    /**
     * Broadcasts a [ServerMessage] to all clients currently connected.
     *
     * @param message The [ServerMessage] that should be broadcast.
     */
    fun broadcastWsMessage(message: ServerMessage) = this.clientLock.read {
        this.connectedClients.values.forEach {
            it.send(message)
        }
    }

    /**
     * Broadcasts a [ServerMessage] to all clients currently connected and observing a specific [RunManager].
     *
     * @param evaluationId The [EvaluationId] identifying the [RunManager] for which clients should receive the message.
     * @param message The [ServerMessage] that should be broadcast.
     */
    fun broadcastWsMessage(evaluationId: EvaluationId, message: ServerMessage) = this.clientLock.read {
        this.runManagerLock.read {
            this.connectedClients.values.filter {
                this.observingClients[evaluationId]?.contains(it) ?: false
            }.forEach {
                it.send(message)
            }
        }
    }

    /**
     * Broadcasts a [ServerMessage] to all clients currently connected and observing a specific [RunManager] and are member of the specified team.
     *
     * @param evaluationId The run ID identifying the [RunManager] for which clients should receive the message.
     * @param teamId The [TeamId] of the relevant team
     * @param message The [ServerMessage] that should be broadcast.
     */
    fun broadcastWsMessage(evaluationId: EvaluationId, teamId: TeamId, message: ServerMessage) = this.clientLock.read {
        val manager = managerForId(evaluationId)
        if (manager != null) {
            val teamMembers = manager.template.teams.filter { it.id eq teamId }.flatMapDistinct { it.users }.asSequence().map { it.userId }.toList()
            this.runManagerLock.read {
                this.connectedClients.values.filter {
                    this.observingClients[evaluationId]?.contains(it) ?: false && AccessManager.userIdForSession(it.sessionId) in teamMembers
                }.forEach {
                    it.send(message)
                }
            }
        }
    }

    /**
     * Stops all runs
     */
    fun stop() {
        this.executor.shutdownNow()
    }


    /**
     * Dumps the given [EvaluationRun] to a file.
     *
     * @param competition [EvaluationRun] that should be dumped.
     */
    fun dump(competition: EvaluationRun) {
        try {
            val file = File("run_dump_${competition.id}.json")
            jacksonObjectMapper().writeValue(file, competition)
            this.logger.info("Wrote current run state to ${file.absolutePath}")
        } catch (e: Exception){
            this.logger.error("Could not write run to disk: ", e)
        }
    }
}
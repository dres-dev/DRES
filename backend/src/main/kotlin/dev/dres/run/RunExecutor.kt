package dev.dres.run

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.WebSocketConnection
import dev.dres.api.rest.types.run.websocket.ClientMessage
import dev.dres.api.rest.types.run.websocket.ClientMessageType
import dev.dres.api.rest.types.run.websocket.ServerMessage
import dev.dres.api.rest.types.run.websocket.ServerMessageType
import dev.dres.data.dbo.DAO
import dev.dres.data.model.UID
import dev.dres.data.model.run.InteractiveSynchronousCompetition
import dev.dres.data.model.run.NonInteractiveCompetition
import dev.dres.data.model.run.interfaces.Competition
import dev.dres.run.validation.interfaces.JudgementValidator
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.read
import dev.dres.utilities.extensions.write
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsHandler
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
 * @version 1.1
 */
object RunExecutor : Consumer<WsHandler> {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    /** Thread Pool Executor which is used to execute the [RunManager]s. */
    private val executor = Executors.newCachedThreadPool()

    /** List of [RunManager] executed by this [RunExecutor]. */
    private val runManagers = HashMap<UID,RunManager>()

    /** List of [JudgementValidator]s registered with this [RunExecutor]. */
    private val judgementValidators = LinkedList<JudgementValidator>()

    /** List of [WsContext] that are currently connected. */
    private val connectedClients = HashSet<WebSocketConnection>()

    /** List of session IDs that are currently observing a competition. */
    private val observingClients = HashMap<UID,MutableSet<WebSocketConnection>>()

    /** Lock for accessing and changing all data structures related to WebSocket clients. */
    private val clientLock = StampedLock()

    /** Lock for accessing and changing all data structures related to [RunManager]s. */
    private val runManagerLock = StampedLock()

    /** Internal array of [Future]s for cleaning after [RunManager]s. See [RunExecutor.cleanerThread]*/
    private val results = HashMap<Future<*>, UID>()

    /** Instance of shared [DAO] used to access [InteractiveSynchronousCompetition]s. */
    lateinit var runs: DAO<Competition>

    /**
     * Initializes this [RunExecutor].
     *
     * @param runs The shared [DAO] used to access [InteractiveSynchronousCompetition]s.
     */
    fun init(runs: DAO<Competition>) {
        this.runs = runs
        this.runs.filter { !it.hasEnded }.forEach { //TODO needs more distinction
            schedule(it)
        }
    }

    fun schedule(competition: Competition) {
        val run = when(competition) {
            is InteractiveSynchronousCompetition -> {
                competition.tasks.forEach { t ->
                    t.submissions.forEach { s -> s.task = t }
                }
                InteractiveSynchronousRunManager(competition)
            }
            is NonInteractiveCompetition -> {
                NonInteractiveRunManager(competition)
            }
            else -> throw NotImplementedError("No matching run manager found for $competition")
        }
        this.schedule(run)
    }

    /** A thread that cleans after [RunManager] have finished. */
    private val cleanerThread = Thread(Runnable {
        while(!this@RunExecutor.executor.isShutdown) {
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
    })

    init {
        this.cleanerThread.priority = Thread.MIN_PRIORITY
        this.cleanerThread.isDaemon = true
        this.cleanerThread.name = "run-manager-cleaner"
        this.cleanerThread.start()
    }

    /**
     * Callback for when registering this [RunExecutor] as handler for Javalin's WebSocket.
     *
     * @param t The [WsHandler] of the WebSocket endpoint.
     */
    override fun accept(t: WsHandler) {
        t.onConnect {
            /* Add WSContext to set of connected clients. */
            this@RunExecutor.clientLock.write {
                this.connectedClients.add(WebSocketConnection(it))
            }
        }
        t.onClose {
            val session = WebSocketConnection(it)
            this@RunExecutor.clientLock.write {
                val connection = WebSocketConnection(it)
                this.connectedClients.remove(connection)
                this.runManagerLock.read {
                    for (m in runManagers) {
                        if (this.observingClients[m.key]?.contains(connection) == true) {
                            this.observingClients[m.key]?.remove(connection)
                            m.value.wsMessageReceived(session, ClientMessage(m.key.string, ClientMessageType.UNREGISTER)) /* Send implicit unregister message associated with a disconnect. */
                        }
                    }
                }
            }
        }
        t.onMessage {
            val message = try {
                it.message(ClientMessage::class.java)
            } catch (e: Exception) {
                logger.warn("Cannot parse WebSocket message: ${e.localizedMessage}")
                return@onMessage
            }
            val session = WebSocketConnection(it)
            logger.debug("Received WebSocket message: $message from ${it.session.policy}")
            this.runManagerLock.read {
                if (this.runManagers.containsKey(message.runId.UID())) {
                    when (message.type) {
                        ClientMessageType.ACK -> {}
                        ClientMessageType.REGISTER -> this@RunExecutor.clientLock.write { this.observingClients[message.runId.UID()]?.add(WebSocketConnection(it)) }
                        ClientMessageType.UNREGISTER -> this@RunExecutor.clientLock.write { this.observingClients[message.runId.UID()]?.remove(WebSocketConnection(it)) }
                        ClientMessageType.PING -> it.send(ServerMessage(message.runId, ServerMessageType.PING))
                    }
                    this.runManagers[message.runId.UID()]!!.wsMessageReceived(session, message) /* Forward message to RunManager. */
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
     * @param runId The ID for which to return the [RunManager].
     * @return Optional [RunManager].
     */
    fun managerForId(runId: UID): RunManager? = this.runManagerLock.read {
        return this.runManagers[runId]
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

        /* Register [RunManager] with AccessManager. */
        AccessManager.registerRunManager(manager)

        /* Setup all the required data structures. */
        this.runManagers[manager.id] = manager
        this.observingClients[manager.id] = HashSet()
        this.results[this.executor.submit(manager)] = manager.id /* Register future for cleanup thread. */
    }

    /**
     * Broadcasts a [ServerMessage] to all clients currently connected.
     *
     * @param message The [ServerMessage] that should be broadcast.
     */
    fun broadcastWsMessage(message: ServerMessage) = this.clientLock.read {
        this.connectedClients.forEach {
            it.send(message)
        }
    }

    /**
     * Broadcasts a [ServerMessage] to all clients currently connected and observing a specific [RunManager].
     *
     * @param runId The run ID identifying the [RunManager] for which clients should received the message.
     * @param message The [ServerMessage] that should be broadcast.
     */
    fun broadcastWsMessage(runId: UID, message: ServerMessage) = this.clientLock.read {
        this.runManagerLock.read {
            this.connectedClients.filter {
                this.observingClients[runId]?.contains(it) ?: false
            }.forEach {
                it.send(message)
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
     * Dumps the given [Competition] to a file.
     *
     * @param competition [Competition] that should be dumped.
     */
    fun dump(competition: Competition) {
        try {
            val file = File("run_dump_${competition.id.string}.json")
            jacksonObjectMapper().writeValue(file, competition)
            this.logger.info("Wrote current run state to ${file.absolutePath}")
        } catch (e: Exception){
            this.logger.error("Could not write run to disk: ", e)
        }
    }
}
package dres.run

import dres.api.rest.AccessManager
import dres.api.rest.types.run.websocket.ClientMessage
import dres.api.rest.types.run.websocket.ClientMessageType
import dres.api.rest.types.run.websocket.ServerMessage
import dres.api.rest.types.run.websocket.ServerMessageType
import dres.data.dbo.DAO
import dres.data.model.run.CompetitionRun
import dres.run.validation.interfaces.JudgementValidator
import dres.utilities.extensions.read
import dres.utilities.extensions.write
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsHandler
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.locks.StampedLock
import java.util.function.Consumer
import kotlin.collections.HashMap

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
    private val runManagers = HashMap<Long,RunManager>()

    /** List of [JudgementValidator]s registered with this [RunExecutor]. */
    private val judgementValidators = LinkedList<JudgementValidator>()

    /** List of [WsContext] that are currently connected. */
    private val connectedClients = HashMap<String, WsContext>()

    /** List of session IDs that are currently observing a competition. */
    private val observingClients = HashMap<Long,MutableSet<String>>()

    /** Lock for accessing and changing all data structures related to WebSocket clients. */
    private val clientLock = StampedLock()

    /** Lock for accessing and changing all data structures related to [RunManager]s. */
    private val runManagerLock = StampedLock()

    /** Internal array of [Future]s for cleaning after [RunManager]s. See [RunExecutor.cleanerThread]*/
    private val results = HashMap<Future<*>, Long>()

    /** Instance of shared [DAO] used to access [CompetitionRun]s. */
    lateinit var runs: DAO<CompetitionRun>

    /**
     * Initializes this [RunExecutor].
     *
     * @param runs The shared [DAO] used to access [CompetitionRun]s.
     */
    fun init(runs: DAO<CompetitionRun>) {
        this.runs = runs
        this.runs.filter { !it.hasEnded }.forEach {
            val run = SynchronousRunManager(it) /* TODO: Distinction between Synchronous and Asynchronous runs. */
            this.schedule(run)
        }
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
                this.connectedClients[it.sessionId] = it
            }
        }
        t.onClose {
            this@RunExecutor.clientLock.write {
                this.connectedClients.remove(it.sessionId)
                this.runManagerLock.read {
                    for (m in runManagers) {
                        if (this.observingClients[m.key]?.contains(it.sessionId) == true) {
                            this.observingClients[m.key]?.remove(it.sessionId)
                            m.value.wsMessageReceived(it.sessionId, ClientMessage(m.key, ClientMessageType.UNREGISTER)) /* Send implicit unregister message associated with a disconnect. */
                        }
                    }
                }
            }
        }
        t.onMessage {
            val message = try{
                it.message(ClientMessage::class.java)
            } catch (e: Exception) {
                logger.warn("Cannot parse WebSocket message: ${e.localizedMessage}")
                return@onMessage
            }
            this.runManagerLock.read {
                if (this.runManagers.containsKey(message.runId)) {
                    when (message.type) {
                        ClientMessageType.ACK -> {}
                        ClientMessageType.REGISTER -> this@RunExecutor.clientLock.write { this.observingClients[message.runId]?.add(it.sessionId) }
                        ClientMessageType.UNREGISTER -> this@RunExecutor.clientLock.write { this.observingClients[message.runId]?.remove(it.sessionId) }
                        ClientMessageType.PING -> it.send(ServerMessage(message.runId, ServerMessageType.PING))
                    }
                    this.runManagers[message.runId]!!.wsMessageReceived(it.sessionId, message) /* Forward message to RunManager. */
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
    fun managerForId(runId: Long): RunManager? = this.runManagerLock.read {
        return this.runManagers[runId]
    }

    /**
     * Schedules a new [RunManager] with this [RunExecutor].
     *
     * @param manager [RunManager] to execute.
     */
    fun schedule(manager: RunManager) = this.runManagerLock.write {
        if (this.runManagers.containsKey(manager.runId)) {
            throw IllegalArgumentException("This RunExecutor already runs a RunManager with the given ID ${manager.runId}. The same RunManager cannot be executed twice!")
        }

        /* Register [RunManager] with AccessManager. */
        AccessManager.registerRunManager(manager)

        /* Setup all the required data structures. */
        this.runManagers[manager.runId] = manager
        this.observingClients[manager.runId] = HashSet()
        this.results[this.executor.submit(manager)] = manager.runId /* Register future for cleanup thread. */
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
     * @param runId The run ID identifying the [RunManager] for which clients should received the message.
     * @param message The [ServerMessage] that should be broadcast.
     */
    fun broadcastWsMessage(runId: Long, message: ServerMessage) = this.clientLock.read {
        this.runManagerLock.read {
            this.connectedClients.values.filter {
                this.observingClients[runId]?.contains(it.sessionId) ?: false
            }.forEach {
                it.send(message)
            }
        }
    }

    /**
     * Stops all runs
     */
    fun stop() {
        executor.shutdown()
    }
}
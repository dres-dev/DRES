package dres.run

import dres.api.rest.types.run.ClientMessage
import dres.api.rest.types.run.ClientMessageType
import dres.api.rest.types.run.ServerMessage
import dres.utilities.extensions.read
import dres.utilities.extensions.write

import io.javalin.websocket.WsContext
import io.javalin.websocket.WsHandler
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
 * @version 1.0
 */
object RunExecutor : Consumer<WsHandler> {

    /** Thread Pool Executor which is used to execute the [RunManager]s. */
    private val executor = Executors.newCachedThreadPool()

    /** List of [RunManager] executed by this [RunExecutor]. */
    private val runManagers = HashMap<Long,RunManager>()

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

    /** A thread that cleans after [RunManager] have finished. */
    private val cleanerThread = Thread(Runnable {
        while(!this@RunExecutor.executor.isShutdown) {
            var stamp = this@RunExecutor.runManagerLock.readLock()
            try {
                this@RunExecutor.results.forEach { (k, v) ->
                    if (k.isDone || k.isCancelled) {
                        stamp = this@RunExecutor.runManagerLock.tryConvertToWriteLock(stamp)
                        if (stamp > -1L) {
                            /* Cleanup. */
                            this@RunExecutor.results.remove(k)
                            this@RunExecutor.runManagers.remove(v)
                            this@RunExecutor.observingClients.remove(v)
                        }
                    }
                }
            } finally {
                this@RunExecutor.runManagerLock.unlock(stamp)
            }
            Thread.sleep(500)
        }
    }, "run-manager-cleaner")

    init {
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
                    for (m in runManagers.keys) {
                        if (this.observingClients[m]?.contains(it.sessionId) == true) {
                            this.observingClients[m]?.remove(it.sessionId)
                            this.runManagers[m]?.wsMessageReceived(ClientMessage(m, ClientMessageType.UNREGISTER)) /* Send implicit unregister message associated with a disconnect. */
                        }
                    }
                }
            }
        }
        t.onMessage {
            val message = it.message(ClientMessage::class.java)
            this.runManagerLock.read {
                if (this.runManagers.containsKey(message.runId)) {
                    when (message.type) {
                        ClientMessageType.ACK -> {}
                        ClientMessageType.REGISTER -> this@RunExecutor.clientLock.write { this.observingClients[message.runId]?.add(it.sessionId) }
                        ClientMessageType.UNREGISTER -> this@RunExecutor.clientLock.write { this.observingClients[message.runId]?.remove(it.sessionId) }
                    }
                    this.runManagers[message.runId]!!.wsMessageReceived(message); /* Forward message to RunManager. */
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
}
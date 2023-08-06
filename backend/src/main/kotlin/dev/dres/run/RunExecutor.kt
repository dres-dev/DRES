package dev.dres.run


import dev.dres.api.rest.types.ViewerInfo
import dev.dres.data.model.config.Config
import dev.dres.data.model.run.*
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.mgmt.cache.CacheManager
import dev.dres.run.validation.interfaces.JudgementValidator
import dev.dres.utilities.extensions.read
import dev.dres.utilities.extensions.write
import io.javalin.websocket.WsContext
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.locks.StampedLock

/**
 * The execution environment for [RunManager]s
 *
 * @author Ralph Gasser
 * @version 1.3.0
 */
object RunExecutor {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    /** Thread Pool Executor which is used to execute the [RunManager]s. */
    private val executor = Executors.newCachedThreadPool()

    /** List of [RunManager] executed by this [RunExecutor]. */
    private val runManagers = HashMap<EvaluationId,RunManager>()

    /** List of [JudgementValidator]s registered with this [RunExecutor]. */
    private val judgementValidators = LinkedList<JudgementValidator>()

    /** List of [WsContext] that are currently connected. */
    private val connectedClients = HashMap<String,ViewerInfo>()

    /** List of session IDs that are currently observing an evaluation. */
    private val observingClients = HashMap<EvaluationId, MutableSet<ViewerInfo>>()

    /** Lock for accessing and changing all data structures related to WebSocket clients. */
    private val clientLock = StampedLock()

    /** Lock for accessing and changing all data structures related to [RunManager]s. */
    private val runManagerLock = StampedLock()

    /** Internal array of [Future]s for cleaning after [RunManager]s. See [RunExecutor.cleanerThread]*/
    private val results = HashMap<Future<*>, EvaluationId>()

    /** Initializes the [RunExecutor] singleton.
     *
     * @param config The [Config] with which DRES was started.
     * @param store The [TransientEntityStore] instance used to access persistent data.
     * @param cache The [CacheManager] instance used to access the media cache.
     */
    fun init(config: Config, store: TransientEntityStore, cache: CacheManager) {
        store.transactional {
            DbEvaluation.filter { (it.ended eq null) }.asSequence().forEach {e ->
                this.schedule(e.toRunManager(store))  /* Re-schedule evaluations. */
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

//    /**
//     * Callback for when registering this [RunExecutor] as handler for Javalin's WebSocket.
//     *
//     * @param t The [WsConfig] of the WebSocket endpoint.
//     */
//    fun accept(t: WsConfig) { //TODO remove
//        t.onConnect {
//            /* Add WSContext to set of connected clients. */
//            this@RunExecutor.clientLock.write {
//                val connection = WebSocketConnection(it)
//                this.connectedClients[connection.httpSessionId] = connection
//            }
//        }
//        t.onClose {
//            val session = WebSocketConnection(it)
//            this@RunExecutor.clientLock.write {
//                val connection = WebSocketConnection(it)
//                this.connectedClients.remove(connection.httpSessionId)
//                this.runManagerLock.read {
//                    for (m in this.runManagers) {
//                        if (this.observingClients[m.key]?.remove(connection) == true) {
//                            m.value.wsMessageReceived(session, ClientMessage(m.key, ClientMessageType.UNREGISTER)) /* Send implicit unregister message associated with a disconnect. */
//                        }
//                    }
//                }
//            }
//        }
//        t.onMessage {
//            val message = try {
//                it.messageAsClass<ClientMessage>()
//            } catch (e: Exception) {
//                logger.warn("Cannot parse WebSocket message: ${e.localizedMessage}")
//                return@onMessage
//            }
//            val session = WebSocketConnection(it)
//            logger.debug("Received WebSocket message: $message from ${it.session.policy}")
//            this.runManagerLock.read {
//                if (this.runManagers.containsKey(message.evaluationId)) {
//                    when (message.type) {
//                        ClientMessageType.ACK -> {}
//                        ClientMessageType.REGISTER -> this@RunExecutor.clientLock.write { this.observingClients[message.evaluationId]?.add(WebSocketConnection(it)) }
//                        ClientMessageType.UNREGISTER -> this@RunExecutor.clientLock.write { this.observingClients[message.evaluationId]?.remove(WebSocketConnection(it)) }
//                        ClientMessageType.PING -> it.send(ServerMessage(message.evaluationId, ServerMessageType.PING))
//                    }
//                    this.runManagers[message.evaluationId]!!.wsMessageReceived(session, message) /* Forward message to RunManager. */
//                }
//            }
//        }
//    }

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

//    /**
//     * Broadcasts a [ServerMessage] to all clients currently connected and observing a specific [RunManager].
//     *
//     * @param message The [ServerMessage] that should be broadcast.
//     */
//    fun broadcastWsMessage(message: ServerMessage) = this.clientLock.read {
//        this.runManagerLock.read {
//            this.connectedClients.values.filter {
//                this.observingClients[message.evaluationId]?.contains(it) ?: false
//            }.forEach {
//                it.send(message)
//            }
//        }
//    }

//    /**
//     * Broadcasts a [ServerMessage] to all clients currently connected and observing a specific [RunManager] and are member of the specified team.
//     *
//     * @param teamId The [TeamId] of the relevant team
//     * @param message The [ServerMessage] that should be broadcast.
//     */
//    fun broadcastWsMessage(teamId: TeamId, message: ServerMessage) = this.clientLock.read {
//        val manager = managerForId(message.evaluationId)
//        if (manager != null) {
//            val teamMembers = manager.template.teams.filter { it.id eq teamId }.flatMapDistinct { it.users }.asSequence().map { it.userId }.toList()
//            this.runManagerLock.read {
//                this.connectedClients.values.filter {
//                    this.observingClients[message.evaluationId]?.contains(it) ?: false && AccessManager.userIdForSession(it.sessionId) in teamMembers
//                }.forEach {
//                    it.send(message)
//                }
//            }
//        }
//    }

    /**
     * Stops all runs
     */
    fun stop() {
        this.executor.shutdownNow()
    }

}
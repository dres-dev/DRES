package dev.dres.run

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.ViewerInfo
import dev.dres.api.rest.types.evaluation.ApiEvaluationOverview
import dev.dres.api.rest.types.evaluation.ApiEvaluationState
import dev.dres.api.rest.types.evaluation.ApiTaskOverview
import dev.dres.api.rest.types.evaluation.ApiTeamTaskOverview
import dev.dres.api.rest.types.evaluation.websocket.ClientMessage
import dev.dres.api.rest.types.evaluation.websocket.ClientMessageType
import dev.dres.api.rest.types.evaluation.websocket.ServerMessage
import dev.dres.api.rest.types.evaluation.websocket.ServerMessageType
import dev.dres.data.model.run.*
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.template.team.TeamId
import dev.dres.mgmt.EvaluationManager
import dev.dres.run.eventstream.*
import dev.dres.run.validation.interfaces.JudgementValidator
import dev.dres.utilities.extensions.sessionToken
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

    /** Session IDs currently observing each evaluation. */
    private val observingClients = ConcurrentHashMap<EvaluationId, MutableSet<String>>()

    /** Bounded FIFO sender per websocket session so a slow observer cannot block event dispatch. */
    private val outboundSenders = ConcurrentHashMap<String, BoundedSerialMessageSender>()

    private val maxPendingWebSocketMessages = 256

    /** Viewer identities registered by each WebSocket session, grouped by evaluation. */
    private val socketViewers = ConcurrentHashMap<String, ConcurrentHashMap<EvaluationId, ViewerInfo>>()

    /** Active WebSocket sessions for each viewer identity, grouped by evaluation. */
    private val viewerSockets = ConcurrentHashMap<EvaluationId, ConcurrentHashMap<ViewerInfo, MutableSet<String>>>()

    /** Serializes registration, heartbeat, and cleanup for viewer WebSocket state. */
    private val viewerSocketLock = Any()

    /** Last client PING time, by WebSocket session and evaluation. */
    private val lastViewerPing = ConcurrentHashMap<String, ConcurrentHashMap<EvaluationId, Long>>()

    private data class DisconnectedViewer(val evaluationId: EvaluationId, val viewer: ViewerInfo, val lastSeen: Long? = null)

    /** Three missed 30-second pings are tolerated before a viewer is marked offline. */
    private const val VIEWER_CONNECTION_TIMEOUT_MS = 90_000L
    /** Offline viewer rows are retained briefly so administrators can see recent disconnects. */
    private const val VIEWER_OFFLINE_RETENTION_MS = 300_000L

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

    /** Runs an operation that requires this evaluation to remain unscheduled. */
    fun <T> withNoActiveManager(evaluationId: EvaluationId, action: () -> T): T = this.runManagerLock.write {
        check(!this.runManagers.containsKey(evaluationId)) {
            "Evaluation $evaluationId is still running. Terminate it first."
        }
        action()
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
        this.observingClients[manager.id] = ConcurrentHashMap.newKeySet()
        this.results[this.executor.submit(manager)] = manager.id
    }

    /** A thread that cleans after [RunManager]s have finished. */
    private val cleanerThread = Thread {
        while (!this@RunExecutor.executor.isShutdown) {
            /* Determine which futures have finished without holding the write lock. */
            val finished = this@RunExecutor.runManagerLock.read {
                this@RunExecutor.results.entries.filter { (k, _) -> k.isDone || k.isCancelled }
            }
            if (finished.isNotEmpty()) {
                /* Acquiring the write lock separately (rather than upgrading from the read lock above,
                 * which ReentrantReadWriteLock does not support and would deadlock). */
                this@RunExecutor.runManagerLock.write {
                    finished.forEach { (k, v) ->
                        logger.info("RunManager $v (done = ${k.isDone}, cancelled = ${k.isCancelled}) will be removed!")
                        this@RunExecutor.results.remove(k)
                        this@RunExecutor.runManagers.remove(v)
                        this@RunExecutor.observingClients.remove(v)
                    }
                }
            }
            val now = System.currentTimeMillis()
            val connectedBefore = now - VIEWER_CONNECTION_TIMEOUT_MS
            val managerSnapshot = this@RunExecutor.runManagerLock.read { HashMap(this@RunExecutor.runManagers) }
            val disconnected = ArrayList<DisconnectedViewer>()
            synchronized(this@RunExecutor.viewerSocketLock) {
                (this@RunExecutor.viewerSockets.keys - managerSnapshot.keys).forEach { evaluationId ->
                    this@RunExecutor.viewerSockets.remove(evaluationId)?.values?.forEach { sessionIds ->
                        sessionIds.forEach { sessionId ->
                            this@RunExecutor.socketViewers[sessionId]?.remove(evaluationId)
                            this@RunExecutor.lastViewerPing[sessionId]?.remove(evaluationId)
                            this@RunExecutor.observingClients[evaluationId]?.remove(sessionId)
                            if (this@RunExecutor.socketViewers[sessionId]?.isEmpty() == true) this@RunExecutor.socketViewers.remove(sessionId)
                            if (this@RunExecutor.lastViewerPing[sessionId]?.isEmpty() == true) this@RunExecutor.lastViewerPing.remove(sessionId)
                        }
                    }
                }
                this@RunExecutor.viewerSockets.forEach { (evaluationId, viewers) ->
                    val manager = managerSnapshot[evaluationId] ?: return@forEach
                    viewers.forEach { (viewer, sessionIds) ->
                        val lastSeen = sessionIds.mapNotNull { sessionId ->
                            this@RunExecutor.lastViewerPing[sessionId]?.get(evaluationId)
                        }.maxOrNull() ?: 0L
                        if (lastSeen < connectedBefore && manager.viewerPresence(viewer)?.connected == true) {
                            /* Mark an idle socket offline, but retain its registration until the
                             * longer retention timeout so a late heartbeat can restore it. */
                            disconnected.add(DisconnectedViewer(evaluationId, viewer, lastSeen))
                        }
                        if (lastSeen < now - VIEWER_OFFLINE_RETENTION_MS) {
                            sessionIds.toList().forEach { sessionId ->
                                this@RunExecutor.socketViewers[sessionId]?.remove(evaluationId, viewer)
                                this@RunExecutor.lastViewerPing[sessionId]?.remove(evaluationId)
                                if (this@RunExecutor.socketViewers[sessionId]?.isEmpty() == true) this@RunExecutor.socketViewers.remove(sessionId)
                                if (this@RunExecutor.lastViewerPing[sessionId]?.isEmpty() == true) this@RunExecutor.lastViewerPing.remove(sessionId)
                                this@RunExecutor.observingClients[evaluationId]?.remove(sessionId)
                            }
                            this@RunExecutor.viewerSockets[evaluationId]?.remove(viewer, sessionIds)
                        }
                    }
                }
            }
            disconnected.forEach { pending ->
                managerSnapshot[pending.evaluationId]?.viewerConnectionChanged(pending.viewer, false, pending.lastSeen ?: now)
            }
            val cutoff = now - VIEWER_OFFLINE_RETENTION_MS
            managerSnapshot.values.forEach { manager -> manager.cleanupViewerPresence(cutoff) }
            Thread.sleep(5_000)
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
            val sessionId = ctx.sessionId()
            this.socketViewers[sessionId] = ConcurrentHashMap()
            lateinit var sender: BoundedSerialMessageSender
            sender = BoundedSerialMessageSender(
                maxPendingMessages = maxPendingWebSocketMessages,
                sendAsync = { text, succeeded, failed ->
                    ctx.session.sendText(text, org.eclipse.jetty.websocket.api.Callback.from(succeeded, failed))
                },
                closeSession = { code, reason ->
                    ctx.session.close(code, reason, org.eclipse.jetty.websocket.api.Callback.NOOP)
                },
                onClosed = { this.removeWebSocketSession(sessionId, sender) }
            )
            this.outboundSenders[sessionId] = sender
            logger.debug("WebSocket client connected: $sessionId")
        }

        ws.onClose { ctx ->
            this.removeWebSocketSession(ctx.sessionId())
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
                val manager = this.runManagers[message.evaluationId]
                if (manager != null) {
                    when (message.type) {
                        ClientMessageType.ACK -> {}
                        ClientMessageType.REGISTER -> {
                            if (AccessManager.canViewEvaluation(ctx.sessionToken(), manager)) {
                                val viewer = this.viewerOf(ctx)
                                if (viewer != null) {
                                    val disconnected = synchronized(this.viewerSocketLock) {
                                        this.observingClients[message.evaluationId]?.add(ctx.sessionId())
                                        this.registerSocketViewer(ctx.sessionId(), message.evaluationId, viewer, manager)
                                    }
                                    disconnected?.let { this.managerForId(it.evaluationId)?.viewerConnectionChanged(it.viewer, false) }
                                }
                            } else {
                                logger.warn("WebSocket session ${ctx.sessionId()} is not permitted to observe evaluation ${message.evaluationId}")
                            }
                        }
                        ClientMessageType.UNREGISTER -> {
                            val disconnected = synchronized(this.viewerSocketLock) {
                                this.observingClients[message.evaluationId]?.remove(ctx.sessionId())
                                this.unregisterSocketViewer(ctx.sessionId(), message.evaluationId)
                            }
                            disconnected?.let { this.managerForId(it.evaluationId)?.viewerConnectionChanged(it.viewer, false) }
                        }
                        ClientMessageType.PING -> {
                            synchronized(this.viewerSocketLock) {
                                var viewer = this.socketViewers[ctx.sessionId()]?.get(message.evaluationId)
                                if (viewer == null && AccessManager.canViewEvaluation(ctx.sessionToken(), manager)) {
                                    viewer = this.viewerOf(ctx)
                                    if (viewer != null) {
                                        this.observingClients[message.evaluationId]?.add(ctx.sessionId())
                                        this.registerSocketViewer(ctx.sessionId(), message.evaluationId, viewer, manager)
                                    }
                                }
                                viewer?.let {
                                    val now = System.currentTimeMillis()
                                    this.lastViewerPing.computeIfAbsent(ctx.sessionId()) { ConcurrentHashMap() }[message.evaluationId] = now
                                    if (manager.viewerPresence(it)?.connected == false) {
                                        manager.viewerConnectionChanged(it, true, now)
                                    } else {
                                        manager.viewerConnectionHeartbeat(it)
                                    }
                                }
                            }
                            this.outboundSenders[ctx.sessionId()]?.send(
                                mapper.writeValueAsString(ServerMessage(message.evaluationId, ServerMessageType.PING))
                            )
                        }
                    }
                }
            }
        }

        ws.onError { ctx ->
            logger.error("WebSocket error for session ${ctx.sessionId()}: ${ctx.error()?.message}")
            this.removeWebSocketSession(ctx.sessionId())
        }
    }

    /**
     * Derives the [ViewerInfo] for a WebSocket session, or returns null if the session carries no
     * resolvable session token. Failures are logged instead of silently dropping the viewer, which
     * previously left viewers connected in the UI but unknown to the backend.
     */
    private fun viewerOf(ctx: WsContext): ViewerInfo? = try {
        ViewerInfo(ctx)
    } catch (e: Exception) {
        logger.warn("Cannot determine viewer identity for WebSocket session ${ctx.sessionId()}: ${e.message}")
        null
    }

    private fun removeWebSocketSession(sessionId: String, expectedSender: BoundedSerialMessageSender? = null) {
        if (expectedSender == null) {
            this.outboundSenders.remove(sessionId)?.cancel()
        } else if (!this.outboundSenders.remove(sessionId, expectedSender)) {
            return
        } else {
            expectedSender.cancel()
        }
        this.observingClients.values.forEach { it.remove(sessionId) }
        this.disconnectSocketViewer(sessionId)
    }

    private fun registerSocketViewer(sessionId: String, evaluationId: EvaluationId, viewer: ViewerInfo, manager: RunManager): DisconnectedViewer? {
        val existing = this.socketViewers[sessionId]?.get(evaluationId)
        if (existing == viewer) {
            val now = System.currentTimeMillis()
            this.lastViewerPing.computeIfAbsent(sessionId) { ConcurrentHashMap() }[evaluationId] = now
            manager.viewerConnectionHeartbeat(viewer)
            return null
        }
        val disconnected = this.unregisterSocketViewer(sessionId, evaluationId)
        this.socketViewers.computeIfAbsent(sessionId) { ConcurrentHashMap() }[evaluationId] = viewer
        this.lastViewerPing.computeIfAbsent(sessionId) { ConcurrentHashMap() }[evaluationId] = System.currentTimeMillis()
        val sockets = this.viewerSockets.computeIfAbsent(evaluationId) { ConcurrentHashMap() }
            .computeIfAbsent(viewer) { ConcurrentHashMap.newKeySet() }
        sockets.add(sessionId)
        manager.viewerConnectionChanged(viewer, true)
        return disconnected
    }

    private fun unregisterSocketViewer(sessionId: String, evaluationId: EvaluationId): DisconnectedViewer? {
        this.lastViewerPing[sessionId]?.remove(evaluationId)
        val viewer = this.socketViewers[sessionId]?.remove(evaluationId) ?: return null
        val sockets = this.viewerSockets[evaluationId]?.get(viewer)
        if (sockets?.remove(sessionId) == true && sockets.isEmpty()) {
            this.viewerSockets[evaluationId]?.remove(viewer, sockets)
            return DisconnectedViewer(evaluationId, viewer)
        }
        return null
    }

    private fun disconnectSocketViewer(sessionId: String) {
        val disconnected = synchronized(this.viewerSocketLock) {
            val evaluationIds = this.socketViewers[sessionId]?.keys?.toList().orEmpty()
            val lastSockets = evaluationIds.mapNotNull { evaluationId -> this.unregisterSocketViewer(sessionId, evaluationId) }
            this.socketViewers.remove(sessionId)
            this.lastViewerPing.remove(sessionId)
            lastSockets
        }
        disconnected.forEach { this.managerForId(it.evaluationId)?.viewerConnectionChanged(it.viewer, false) }
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
        val observers = this.observingClients[message.evaluationId]?.toSet() ?: emptySet()
        observers.forEach { sessionId ->
            this.outboundSenders[sessionId]?.send(json)
        }
    }

    /**
     * Builds both [ApiEvaluationState] and [ApiEvaluationOverview] in a single readonly
     * transaction. Used when a task-state event needs both payloads so we avoid opening
     * two separate transactions for the same snapshot.
     */
    private fun InteractiveRunManager.buildStateAndOverview(): Pair<ApiEvaluationState?, ApiEvaluationOverview?> =
        runCatching {
            this.store.transactional(readonly = true) {
                Pair(
                    ApiEvaluationState(this@buildStateAndOverview, RunActionContext.INTERNAL),
                    ApiEvaluationOverview.of(
                        this@buildStateAndOverview,
                        EvaluationManager.verdictCountsInCurrentTransaction(this@buildStateAndOverview.id)
                    )
                )
            }
        }.onFailure { logger.warn("Failed to build state+overview diff for WS message: ${it.message}") }
         .getOrElse { Pair(null, null) }

    /**
     * Builds an [ApiEvaluationOverview] for the given [InteractiveRunManager] inside a readonly
     * transaction. Returns null on any failure so callers can fall back to an HTTP fetch.
     */
    private fun InteractiveRunManager.buildOverview(): ApiEvaluationOverview? = runCatching {
        this.store.transactional(readonly = true) {
            ApiEvaluationOverview.of(
                this@buildOverview,
                EvaluationManager.verdictCountsInCurrentTransaction(this@buildOverview.id)
            )
        }
    }.onFailure { logger.warn("Failed to build overview diff for WS message: ${it.message}") }.getOrNull()

    /**
     * Builds an [ApiTeamTaskOverview] scoped to the given [teamId] inside a readonly transaction.
     *
     * Used so that a submission from a single team only ever broadcasts that team's overview
     * instead of rebuilding and sending every team's overview. Returns null on any failure so
     * callers can fall back to an HTTP fetch.
     */
    private fun InteractiveRunManager.buildTeamOverview(teamId: TeamId): ApiTeamTaskOverview? = runCatching {
        this.store.transactional(readonly = true) {
            val tasks = when (val manager = this@buildTeamOverview) {
                is InteractiveSynchronousRunManager -> manager.evaluation.taskRuns.asSequence().map { ApiTaskOverview(it) }.toList()
                is InteractiveAsynchronousRunManager -> manager.evaluation.taskRuns.asSequence().filter { it.teamId == teamId }.map { ApiTaskOverview(it) }.toList()
                else -> throw IllegalStateException("Unsupported run manager type") //should never happen
            }
            ApiTeamTaskOverview(teamId, tasks)
        }
    }.onFailure { logger.warn("Failed to build team overview diff for WS message: ${it.message}") }.getOrNull()

    /**
     * Maps a [StreamEvent] to the [ServerMessage] that should be broadcast, or null
     * if the event type requires no WebSocket notification.
     *
     * When the event carries enough information, [ServerMessage.state] and/or
     * [ServerMessage.overview] are populated so that receivers can update their local
     * state without issuing a separate HTTP request. [SubmissionEvent]s populate the more
     * narrowly scoped [ServerMessage.teamOverview] instead, since only one team's overview
     * actually changes as a result.
     */
    internal fun eventToMessage(event: StreamEvent): ServerMessage? = when (event) {
        is RunStartEvent -> ServerMessage(event.runId, ServerMessageType.COMPETITION_START)

        is RunEndEvent -> ServerMessage(event.runId, ServerMessageType.COMPETITION_END)

        is TaskPrepareEvent -> ServerMessage(event.runId, ServerMessageType.TASK_PREPARE, event.taskId)

        is TaskStartEvent -> {
            val manager = this.runManagerLock.read { runManagers[event.runId] as? InteractiveRunManager }
            val (state, overview) = manager?.buildStateAndOverview() ?: Pair(null, null)
            ServerMessage(event.runId, ServerMessageType.TASK_START, event.taskId, state = state, overview = overview)
        }

        is TaskEndEvent -> {
            val manager = this.runManagerLock.read { runManagers[event.runId] as? InteractiveRunManager }
            val (state, overview) = manager?.buildStateAndOverview() ?: Pair(null, null)
            ServerMessage(event.runId, ServerMessageType.TASK_END, event.taskId, state = state, overview = overview)
        }

        is ScoreUpdateEvent -> {
            val manager = this.runManagerLock.read { runManagers[event.runId] as? InteractiveRunManager }
            ServerMessage(event.runId, ServerMessageType.COMPETITION_UPDATE, overview = manager?.buildOverview())
        }

        is SubmissionEvent -> {
            val manager = this.runManagerLock.read { runManagers[event.runId] as? InteractiveRunManager }
            val teamOverview = event.submission.teamId?.let { manager?.buildTeamOverview(it) }
            ServerMessage(event.runId, ServerMessageType.TASK_UPDATED, teamOverview = teamOverview)
        }

        is ViewerUpdateEvent -> ServerMessage(event.runId, ServerMessageType.VIEWER_UPDATE)

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

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

    /** WebSocket sessions currently connected, keyed by session ID. */
    private val connectedClients = ConcurrentHashMap<String, WsContext>()

    /** Session IDs currently observing each evaluation. */
    private val observingClients = ConcurrentHashMap<EvaluationId, MutableSet<String>>()

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
            this.connectedClients[ctx.sessionId()] = ctx
            logger.debug("WebSocket client connected: ${ctx.sessionId()}")
        }

        ws.onClose { ctx ->
            this.connectedClients.remove(ctx.sessionId())
            this.observingClients.values.forEach { it.remove(ctx.sessionId()) }
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
                                this.observingClients[message.evaluationId]?.add(ctx.sessionId())
                            } else {
                                logger.warn("WebSocket session ${ctx.sessionId()} is not permitted to observe evaluation ${message.evaluationId}")
                            }
                        }
                        ClientMessageType.UNREGISTER -> this.observingClients[message.evaluationId]?.remove(ctx.sessionId())
                        ClientMessageType.PING -> ctx.send(
                            mapper.writeValueAsString(ServerMessage(message.evaluationId, ServerMessageType.PING))
                        )
                    }
                }
            }
        }

        ws.onError { ctx ->
            logger.error("WebSocket error for session ${ctx.sessionId()}: ${ctx.error()?.message}")
            this.connectedClients.remove(ctx.sessionId())
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
        val observers = this.observingClients[message.evaluationId]?.toSet() ?: emptySet()
        observers.forEach { sessionId ->
            try {
                this.connectedClients[sessionId]?.send(json)
            } catch (e: Exception) {
                logger.warn("Failed to send WebSocket message to session $sessionId: ${e.message}")
            }
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
                    ApiEvaluationOverview.of(this@buildStateAndOverview)
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
            ApiEvaluationOverview.of(this@buildOverview)
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

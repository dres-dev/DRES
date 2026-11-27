package dres.run

import dev.dres.DRES
import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.ViewerInfo
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.config.Config
import dev.dres.data.model.run.InteractiveSynchronousEvaluation
import dev.dres.data.model.run.RunActionContext
import dev.dres.mgmt.EvaluationManager
import dev.dres.mgmt.TemplateManager
import dev.dres.run.InteractiveRunManager
import dev.dres.run.InteractiveSynchronousRunManager
import dev.dres.run.RunExecutor
import dev.dres.run.eventstream.EventStreamProcessor
import dev.dres.run.eventstream.RunStartEvent
import dres.integration.AbstractDresIntegrationTest
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.ws
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.CompletionStage
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * End-to-end tests for the viewer synchronisation channel:
 * `WebSocket handshake -> session resolution -> REGISTER/PING -> viewer presence -> READY -> task start broadcast`.
 *
 * These tests go through the real Javalin wiring that `RestApi` installs
 * (`path("api") { ws("ws/run", RunExecutor::accept) }` plus the session `routes.before` and the
 * `routes.beforeMatched` access gate) and a real [InteractiveSynchronousRunManager], so a regression in
 * any of those layers fails here.
 *
 * Documented regression: viewers connected ("connected" in the browser UI) but the backend never
 * registered them as present, no events were delivered and the run could not leave PREPARING.
 * Root cause: `routes.before` handlers do not run for WebSocket upgrade requests, so the `session`
 * attribute that `WsContext.sessionToken()` reads was always null (see [WsUpgradeProbeTest]).
 */
class ViewerWebSocketIntegrationTest : AbstractDresIntegrationTest() {

    // ── WebSocket client ──────────────────────────────────────────────────────

    private class Listener : WebSocket.Listener {
        val received = LinkedBlockingQueue<String>()
        override fun onOpen(webSocket: WebSocket) { webSocket.request(1) }
        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
            received.add(data.toString())
            webSocket.request(1)
            return null
        }
    }

    private val http: HttpClient = HttpClient.newHttpClient()
    private val started = mutableListOf<Javalin>()

    private fun startServer(): Int {
        val port = ServerSocket(0).use { it.localPort }
        val app = Javalin.create { config ->
            config.routes.apiBuilder { path("api") { ws("ws/run", RunExecutor::accept) } }
            /* Mirrors RestApi.kt: cookie and ?session= both establish the session attribute. */
            config.routes.before { ctx ->
                ctx.cookie(AccessManager.SESSION_COOKIE_NAME)?.let { ctx.attribute("session", it) }
                ctx.queryParam("session")?.let { ctx.attribute("session", it) }
            }
            config.routes.beforeMatched { ctx ->
                if (!AccessManager.hasAccess(ctx)) {
                    ctx.status(401)
                    ctx.skipRemainingHandlers()
                }
            }
        }.start(port)
        started += app
        return port
    }

    @AfterEach
    fun stopServers() {
        started.forEach { runCatching { it.stop() } }
        started.clear()
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private fun buildSyncRun(): Pair<InteractiveSynchronousEvaluation, String> {
        TemplateManager.init(store)
        EvaluationManager.init(store)
        val collection = createTestCollection()
        val template = createTemplateShell("ws-${UUID.randomUUID()}", teamCount = 1)
        val group = addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_ITEM")
        addTask(template, group, collection, "Task-0", idx = 0)
        val dbEvaluation = createEvaluation(template, "ws-run-${UUID.randomUUID()}", "INTERACTIVE_SYNCHRONOUS")
        val evaluation = store.transactional(true) { InteractiveSynchronousEvaluation(store, dbEvaluation) }
        val taskTemplateId = store.transactional(true) { evaluation.getCurrentTaskTemplate().id!! }
        return evaluation to taskTemplateId
    }

    /** Registers a fresh session for a user with the given [role] and returns its token. */
    private fun session(role: ApiRole): String {
        /* createTestUser() opens its own write transaction; wrapping it in a read-only one discards the entity. */
        val user = createTestUser()
        val (userId, username) = store.transactional(true) { user.id!! to user.username }
        val token = "ws-test-${UUID.randomUUID()}"
        AccessManager.registerUserForSession(token, ApiUser(id = userId, username = username, role = role))
        return token
    }

    private fun connect(port: Int, sessionToken: String?): Pair<WebSocket, Listener> {
        val listener = Listener()
        val builder = http.newWebSocketBuilder()
        if (sessionToken != null) builder.header("Cookie", "${AccessManager.SESSION_COOKIE_NAME}=$sessionToken")
        val ws = builder.buildAsync(URI("ws://localhost:$port/api/ws/run"), listener).get(10, TimeUnit.SECONDS)
        return ws to listener
    }

    private fun clientMessage(evaluationId: String, type: String) =
        """{"evaluationId":"$evaluationId","type":"$type"}"""

    private fun awaitMessage(listener: Listener, timeoutMs: Long = 10_000, predicate: (String) -> Boolean): String? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0) break
            val msg = listener.received.poll(remaining, TimeUnit.MILLISECONDS) ?: break
            if (predicate(msg)) return msg
        }
        return null
    }

    private fun awaitCondition(timeoutMs: Long = 10_000, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(25)
        }
        return condition()
    }

    private fun awaitViewer(manager: InteractiveRunManager): ViewerInfo? {
        awaitCondition { manager.viewerSnapshot().isNotEmpty() }
        return manager.viewerSnapshot().keys.firstOrNull()
    }

    /** Schedules a manager, runs [body], and always terminates it again. */
    private fun withManager(body: (InteractiveRunManager, String) -> Unit) {
        val (evaluation, taskTemplateId) = buildSyncRun()
        val manager = store.transactional(true) { InteractiveSynchronousRunManager(evaluation, store) }
        RunExecutor.schedule(manager)
        try {
            body(manager, taskTemplateId)
        } finally {
            runCatching { manager.end(RunActionContext.INTERNAL) }
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * Core regression: REGISTER must make the backend know the viewer and mark it connected.
     * Before the fix `ctx.sessionToken()` was null on the WsContext, so `canViewEvaluation` denied access,
     * the registration was dropped with a warning only, and the admin viewer list never turned green.
     */
    @Test
    fun `REGISTER marks the viewer connected in the backend`() = withManager { manager, _ ->
        val port = startServer()
        val (ws, _) = connect(port, session(ApiRole.ADMIN))

        ws.sendText(clientMessage(manager.id, "REGISTER"), true).get(5, TimeUnit.SECONDS)

        val viewer = awaitViewer(manager)
        assertNotNull(viewer, "Backend did not register the viewer after REGISTER")
        assertEquals(true, manager.viewerPresence(viewer!!)?.connected, "Viewer presence must be connected")
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)
    }

    /** The frontend pings every 30s; a PING must register the viewer too and always be answered. */
    @Test
    fun `PING registers the viewer and is answered`() = withManager { manager, _ ->
        val port = startServer()
        val (ws, listener) = connect(port, session(ApiRole.ADMIN))

        ws.sendText(clientMessage(manager.id, "PING"), true).get(5, TimeUnit.SECONDS)

        val reply = awaitMessage(listener) { it.contains("PING") }
        assertNotNull(reply, "PING must be answered")
        assertNotNull(awaitViewer(manager), "PING must register the viewer")
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)
    }

    /** A session sent as `?session=` (used by clients that cannot set cookies) must work as well. */
    @Test
    fun `query parameter session is honoured`() = withManager { manager, _ ->
        val port = startServer()
        val token = session(ApiRole.ADMIN)
        val listener = Listener()
        val ws = http.newWebSocketBuilder()
            .buildAsync(URI("ws://localhost:$port/api/ws/run?session=$token"), listener)
            .get(10, TimeUnit.SECONDS)

        ws.sendText(clientMessage(manager.id, "REGISTER"), true).get(5, TimeUnit.SECONDS)

        assertNotNull(awaitViewer(manager), "Query-parameter session must be honoured")
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)
    }

    /** Anonymous clients may open the socket, but must never appear as a viewer. */
    @Test
    fun `anonymous REGISTER does not create a viewer`() = withManager { manager, _ ->
        val port = startServer()
        val (ws, _) = connect(port, null)

        ws.sendText(clientMessage(manager.id, "REGISTER"), true).get(5, TimeUnit.SECONDS)
        Thread.sleep(750)

        assertTrue(manager.viewerSnapshot().isEmpty(), "Anonymous REGISTER must not create a viewer")
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)
    }

    /** A session that is not associated with the evaluation must not be able to observe it. */
    @Test
    fun `session without access cannot register`() = withManager { manager, _ ->
        val port = startServer()
        /* A participant that is not a member of any team cannot view this evaluation. */
        val (ws, _) = connect(port, session(ApiRole.PARTICIPANT))

        ws.sendText(clientMessage(manager.id, "REGISTER"), true).get(5, TimeUnit.SECONDS)
        Thread.sleep(750)

        assertTrue(manager.viewerSnapshot().isEmpty(), "Foreign session must not become a viewer")
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)
    }

    /**
     * A REGISTER for a run that is not (or no longer) known must be answered, not swallowed.
     *
     * Before this the backend logged a warning and dropped the message, so a viewer that arrived a
     * moment too late kept an open socket, believed it was subscribed, and waited for events that
     * could never arrive.
     */
    @Test
    fun `REGISTER for an unknown evaluation is answered with an error`() = withManager { _, _ ->
        val port = startServer()
        val (ws, listener) = connect(port, session(ApiRole.ADMIN))

        ws.sendText(clientMessage("no-such-evaluation", "REGISTER"), true).get(5, TimeUnit.SECONDS)

        val reply = awaitMessage(listener) { it.contains("ERROR") }
        assertNotNull(reply, "An unknown evaluation must be reported instead of silently ignored")
        assertTrue(reply!!.contains("no-such-evaluation"), "The error must name the evaluation: $reply")
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)
    }

    /** A rejected REGISTER must say why, otherwise the client waits for events that will never come. */
    @Test
    fun `REGISTER without permission is answered with an error`() = withManager { manager, _ ->
        val port = startServer()
        val (ws, listener) = connect(port, session(ApiRole.PARTICIPANT))

        ws.sendText(clientMessage(manager.id, "REGISTER"), true).get(5, TimeUnit.SECONDS)

        val reply = awaitMessage(listener) { it.contains("ERROR") }
        assertNotNull(reply, "A denied registration must be reported")
        assertTrue(
            reply!!.contains("not permitted") || reply.contains("could not be resolved"),
            "The error must explain the rejection: $reply"
        )
        assertTrue(manager.viewerSnapshot().isEmpty(), "A denied registration must not create a viewer")
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)
    }

    /** A PING is proof of life: even a rejected one is answered, but as an error rather than a plain PING. */
    @Test
    fun `PING without permission is answered with an error`() = withManager { manager, _ ->
        val port = startServer()
        val (ws, listener) = connect(port, null)

        ws.sendText(clientMessage(manager.id, "PING"), true).get(5, TimeUnit.SECONDS)

        val reply = awaitMessage(listener) { it.contains("ERROR") || it.contains("PING") }
        assertNotNull(reply, "PING must always be answered")
        assertTrue(reply!!.contains("ERROR"), "An unregistered PING must be answered with an error: $reply")
        assertTrue(manager.viewerSnapshot().isEmpty(), "An anonymous PING must not create a viewer")
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)
    }

    /** Closing the socket must mark the viewer offline instead of leaving it stuck as connected. */
    @Test
    fun `closing the socket marks the viewer disconnected`() = withManager { manager, _ ->
        val port = startServer()
        val (ws, _) = connect(port, session(ApiRole.ADMIN))

        ws.sendText(clientMessage(manager.id, "REGISTER"), true).get(5, TimeUnit.SECONDS)
        val viewer = awaitViewer(manager)
        assertNotNull(viewer)

        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)

        assertTrue(
            awaitCondition { manager.viewerPresence(viewer!!)?.connected == false },
            "Viewer must be marked disconnected after the socket closed"
        )
    }

    /** Events published on the event stream must reach a registered viewer. */
    @Test
    fun `registered viewer receives broadcast events`() = withManager { manager, _ ->
        val port = startServer()
        val (ws, listener) = connect(port, session(ApiRole.ADMIN))

        ws.sendText(clientMessage(manager.id, "REGISTER"), true).get(5, TimeUnit.SECONDS)
        assertNotNull(awaitViewer(manager))

        RunExecutor.handleStreamEvent(RunStartEvent(manager.id, manager.template))

        assertNotNull(
            awaitMessage(listener) { it.contains("COMPETITION_START") },
            "Registered viewer must receive broadcast events"
        )
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)
    }

    /**
     * The full synchronisation chain: a viewer registers, reports READY, and the run manager must
     * start the task and broadcast TASK_START to that viewer.
     */
    @Test
    fun `READY advances the task and broadcasts TASK_START`() = withManager { manager, taskTemplateId ->
        val port = startServer()
        val (ws, listener) = connect(port, session(ApiRole.ADMIN))

        ws.sendText(clientMessage(manager.id, "REGISTER"), true).get(5, TimeUnit.SECONDS)
        val viewer = awaitViewer(manager)
        assertNotNull(viewer)

        manager.start(RunActionContext.INTERNAL)
        manager.startTask(RunActionContext.INTERNAL)

        manager.viewerReady(taskTemplateId, RunActionContext.INTERNAL, viewer!!)

        val startMessage = awaitMessage(listener, 20_000) { it.contains("TASK_START") }
        assertNotNull(startMessage, "Viewer must be told that the task started")
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)
    }

    /** After a viewer disconnects mid-preparation the run must not stay blocked forever. */
    @Test
    fun `disconnected viewer stops counting towards readiness`() = withManager { manager, taskTemplateId ->
        val port = startServer()
        val (ws, _) = connect(port, session(ApiRole.ADMIN))

        ws.sendText(clientMessage(manager.id, "REGISTER"), true).get(5, TimeUnit.SECONDS)
        val viewer = awaitViewer(manager)
        assertNotNull(viewer)

        manager.start(RunActionContext.INTERNAL)
        manager.startTask(RunActionContext.INTERNAL)
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)

        assertTrue(awaitCondition { manager.viewerPresence(viewer!!)?.connected == false })
        assertFalse(
            manager.viewers()[viewer] == true,
            "A disconnected viewer must not be reported as ready"
        )
    }

    companion object {
        init {
            /* EventStreamProcessor resolves DRES.CONFIG in its object initialiser. */
            val events = Files.createTempDirectory("dres-ws-events-")
            val audit = Files.createTempDirectory("dres-ws-audit-")
            DRES.CONFIG = Config(eventsLocation = events, auditLocation = audit)
            DRES.AUDIT_LOG_ROOT = audit
            /* Mirrors DRES.start(): the executor is the event-stream handler that broadcasts to viewers. */
            EventStreamProcessor.register(RunExecutor)
            EventStreamProcessor.init()
        }
    }
}

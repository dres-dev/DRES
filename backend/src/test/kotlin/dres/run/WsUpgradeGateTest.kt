package dres.run

import dev.dres.DRES
import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.WsUpgradeGate
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.config.Config
import dev.dres.run.RunExecutor
import dres.integration.AbstractDresIntegrationTest
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.ws
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.CompletionStage
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * Tests the WebSocket upgrade gate (`WsUpgradeGate`) that `RestApi` installs for `/api/ws/run`.
 *
 * The gate exists because `routes.before` and `routes.beforeMatched` never run for upgrades, so the
 * HTTP access gate cannot protect the socket. `WsUpgradeGateProbeTest` documents the framework
 * behaviour; this test pins the resulting policy:
 *
 *  - no session -> the upgrade is refused (previously the socket opened and was silently useless),
 *  - an unknown session token -> refused,
 *  - a valid session cookie -> accepted, and the session is visible to the socket handler,
 *  - a valid `?session=` -> accepted (client builders cannot always set cookies),
 *  - a cookie wins over a query parameter with a different value.
 */
class WsUpgradeGateTest : AbstractDresIntegrationTest() {

    private val http: HttpClient = HttpClient.newHttpClient()
    private val started = mutableListOf<Javalin>()

    /** Session tokens observed by the socket handler, one entry per accepted connection. */
    private val observedSessions = CopyOnWriteArrayList<String>()

    private class Listener : WebSocket.Listener {
        override fun onOpen(webSocket: WebSocket) {
            webSocket.request(1)
        }

        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
            webSocket.request(1)
            return null
        }
    }

    companion object {
        init {
            /* RunExecutor and EventStreamProcessor need these before any manager is scheduled. */
            DRES.CONFIG = Config(eventsLocation = Files.createTempDirectory("dres-ws-gate-events-"))
            DRES.AUDIT_LOG_ROOT = Files.createTempDirectory("dres-ws-gate-audit-")
        }
    }

    @BeforeEach
    fun resetObservations() {
        this.observedSessions.clear()
    }

    @AfterEach
    fun stopServers() {
        this.started.forEach { runCatching { it.stop() } }
        this.started.clear()
    }

    /** Mounts the production wiring: the gate as `wsBeforeUpgrade` plus the real socket handler. */
    private fun startServer(): Int {
        val port = ServerSocket(0).use { it.localPort }
        val app = Javalin.create { config ->
            config.routes.apiBuilder { path("api") { ws("ws/run", RunExecutor::accept) } }
            config.routes.wsBeforeUpgrade("/api/ws/run", WsUpgradeGate::gate)
            /* Records what the socket handler sees, so a test can assert attribute propagation. */
            config.routes.wsBeforeUpgrade("/api/ws/run") { ctx ->
                this.observedSessions.add(ctx.attribute<String>("session") ?: "<none>")
            }
        }.start(port)
        this.started += app
        return port
    }

    private fun session(role: ApiRole): String {
        val user = createTestUser()
        val (id, name) = store.transactional(true) { user.id!! to user.username }
        val token = "ws-gate-${UUID.randomUUID()}"
        AccessManager.registerUserForSession(token, ApiUser(id = id, username = name, role = role))
        return token
    }

    private fun Uri(port: Int, query: String = "") = URI("ws://localhost:$port/api/ws/run$query")

    /** Returns the socket, or null when the upgrade was refused. */
    private fun connect(port: Int, cookie: String? = null, query: String = ""): WebSocket? {
        val builder = this.http.newWebSocketBuilder()
        if (cookie != null) builder.header("Cookie", cookie)
        return try {
            builder.buildAsync(Uri(port, query), Listener()).get(10, TimeUnit.SECONDS)
        } catch (e: ExecutionException) {
            null
        }
    }

    @Test
    fun `an upgrade without a session is refused`() {
        val port = this.startServer()

        val socket = this.connect(port)

        assertEquals(null, socket, "An anonymous upgrade must be refused")
        assertTrue(this.observedSessions.isEmpty(), "The socket handler must not run for a refused upgrade")
    }

    @Test
    fun `an upgrade with an unknown session is refused`() {
        val port = this.startServer()

        val socket = this.connect(port, cookie = "${AccessManager.SESSION_COOKIE_NAME}=not-a-real-session")

        assertEquals(null, socket, "An unknown session must not be able to open a socket")
        assertTrue(this.observedSessions.isEmpty())
    }

    @Test
    fun `an upgrade with an unknown session in the query is refused`() {
        val port = this.startServer()

        val socket = this.connect(port, query = "?session=not-a-real-session")

        assertEquals(null, socket, "An unknown session passed as a query parameter must not be accepted")
        assertTrue(this.observedSessions.isEmpty())
    }

    @Test
    fun `a valid session cookie is accepted and visible to the socket handler`() {
        val token = this.session(ApiRole.ADMIN)
        val port = this.startServer()

        val socket = this.connect(port, cookie = "${AccessManager.SESSION_COOKIE_NAME}=$token")

        assertNotNull(socket, "A registered session must be able to open a socket")
        assertTrue(
            this.awaitCondition { this.observedSessions.isNotEmpty() },
            "The socket handler did not run for the accepted upgrade"
        )
        assertEquals(token, this.observedSessions.first(), "The gate must hand the session to the socket handler")
        socket!!.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)
    }

    @Test
    fun `a valid session query parameter is accepted`() {
        val token = this.session(ApiRole.VIEWER)
        val port = this.startServer()

        val socket = this.connect(port, query = "?session=$token")

        assertNotNull(socket, "Client builders that cannot set cookies must still be able to connect")
        assertTrue(this.awaitCondition { this.observedSessions.isNotEmpty() })
        assertEquals(token, this.observedSessions.first())
        socket!!.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)
    }

    @Test
    fun `the cookie takes precedence over the query parameter`() {
        val cookieSession = this.session(ApiRole.ADMIN)
        val querySession = this.session(ApiRole.JUDGE)
        val port = this.startServer()

        val socket = this.connect(
            port,
            cookie = "${AccessManager.SESSION_COOKIE_NAME}=$cookieSession",
            query = "?session=$querySession"
        )

        assertNotNull(socket)
        assertTrue(this.awaitCondition { this.observedSessions.isNotEmpty() })
        assertEquals(
            cookieSession,
            this.observedSessions.first(),
            "A browser sends its cookie automatically; it must win over a possibly stale query parameter"
        )
        socket!!.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)
    }

    /**
     * End-to-end: a session that passes the gate is also allowed to register as a viewer, so the
     * gate and the per-message authorisation agree instead of contradicting each other.
     */
    @Test
    fun `a socket accepted by the gate can register as a viewer`() {
        val token = this.session(ApiRole.ADMIN)
        val (evaluation, _) = this.buildSyncRun()
        val manager = store.transactional(true) { dev.dres.run.InteractiveSynchronousRunManager(evaluation, store) }
        RunExecutor.schedule(manager)
        val port = this.startServer()

        try {
            val socket = this.connect(port, cookie = "${AccessManager.SESSION_COOKIE_NAME}=$token")
            assertNotNull(socket, "A registered session must be able to open a socket")

            socket!!.sendText("""{"evaluationId":"${manager.id}","type":"REGISTER"}""", true).get(5, TimeUnit.SECONDS)

            val deadline = System.currentTimeMillis() + 10_000
            while (manager.viewerSnapshot().isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(25)
            assertTrue(manager.viewerSnapshot().isNotEmpty(), "The gate-accepted socket must be able to register")
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)
        } finally {
            runCatching { manager.end(dev.dres.data.model.run.RunActionContext.INTERNAL) }
        }
    }

    /** Builds a synchronous run so a socket has something to register against. */
    private fun buildSyncRun(): Pair<dev.dres.data.model.run.InteractiveSynchronousEvaluation, String> {
        dev.dres.mgmt.TemplateManager.init(store)
        dev.dres.mgmt.EvaluationManager.init(store)
        val collection = createTestCollection()
        val template = createTemplateShell("ws-gate-${UUID.randomUUID()}", teamCount = 1)
        val group = addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_ITEM")
        addTask(template, group, collection, "Task-0", idx = 0)
        val dbEvaluation = createEvaluation(template, "ws-gate-run-${UUID.randomUUID()}", "INTERACTIVE_SYNCHRONOUS")
        val evaluation = store.transactional(true) { dev.dres.data.model.run.InteractiveSynchronousEvaluation(store, dbEvaluation) }
        return evaluation to evaluation.getCurrentTaskTemplate().id!!
    }

    private fun awaitCondition(timeoutMs: Long = 5_000, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(25)
        }
        return condition()
    }
}

package dres.run

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.ws
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletionStage
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * Executable documentation for the WebSocket upgrade phase of Javalin 7.2.3.
 *
 * The media/preview authorization work and the session fix both depend on knowing exactly which
 * handlers do or do not run before an upgrade. These probes answer three questions that cannot be
 * settled by reading the Javalin sources comfortably:
 *
 *  1. Does `wsBeforeUpgrade` run for an upgrade request, unlike `routes.before` / `routes.beforeMatched`?
 *  2. Do attributes set there reach the [`io.javalin.websocket.WsContext`] used by the socket handlers?
 *  3. Does `skipRemainingHandlers()` inside it actually reject the upgrade?
 *
 * The answers drive the access gate in `RestApi`: the gate has to resolve the session itself, because
 * nothing sets the `session` attribute for upgrades.
 */
class WsUpgradeGateProbeTest {

    private class Listener : WebSocket.Listener {
        val received = CopyOnWriteArrayList<String>()
        var closed = false

        override fun onOpen(webSocket: WebSocket) {
            webSocket.request(1)
        }

        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
            received.add(data.toString())
            webSocket.request(1)
            return null
        }

        override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
            closed = true
            return null
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            closed = true
        }
    }

    private fun <T> withServer(
        configure: (io.javalin.config.JavalinConfig) -> Unit,
        body: (Int) -> T
    ): T = this.withServer(configure, { ws -> ws.onMessage { ctx -> ctx.send("hello") } }, body)

    private fun <T> withServer(
        configure: (io.javalin.config.JavalinConfig) -> Unit,
        wsHandlers: (io.javalin.websocket.WsConfig) -> Unit,
        body: (Int) -> T
    ): T {
        val port = ServerSocket(0).use { it.localPort }
        val app = Javalin.create { config ->
            config.routes.apiBuilder { path("api") { ws("ws/run", wsHandlers) } }
            configure(config)
        }.start(port)
        return try {
            body(port)
        } finally {
            app.stop()
        }
    }

    private fun connect(port: Int, cookie: String? = null, query: String = ""): Pair<WebSocket?, Listener> {
        val listener = Listener()
        val builder = HttpClient.newHttpClient().newWebSocketBuilder()
        if (cookie != null) builder.header("Cookie", cookie)
        return try {
            builder.buildAsync(URI("ws://localhost:$port/api/ws/run$query"), listener).get(10, TimeUnit.SECONDS) to listener
        } catch (e: Exception) {
            null to listener
        }
    }

    @Test
    fun `wsBeforeUpgrade runs before the upgrade and sees the upgrade request`() {
        val seen = CopyOnWriteArrayList<String>()
        this.withServer({ config ->
            config.routes.wsBeforeUpgrade("/api/ws/run") { ctx ->
                seen.add("path=${ctx.path()} cookie=${ctx.cookie("SESSIONID")} query=${ctx.queryParam("session")}")
                ctx.attribute("session", "from-upgrade-handler")
            }
        }) { port ->
            this.connect(port, "SESSIONID=probe-cookie", "?session=probe-query")
            println("PROBE_UPGRADE: " + seen.joinToString(" | "))
            assertEquals(1, seen.size, "wsBeforeUpgrade should run exactly once per upgrade")
            assertEquals("path=/api/ws/run cookie=probe-cookie query=probe-query", seen.first())
        }
    }

    @Test
    fun `attribute set in wsBeforeUpgrade is visible on the websocket context`() {
        val observed = CopyOnWriteArrayList<String>()
        this.withServer({ config ->
            config.routes.wsBeforeUpgrade("/api/ws/run") { ctx -> ctx.attribute("session", "from-upgrade-handler") }
        }, { ws ->
            ws.onConnect { ctx -> observed.add("connect attribute=${ctx.attribute<String>("session")}") }
            ws.onMessage { ctx ->
                observed.add("message attribute=${ctx.attribute<String>("session")}")
                ctx.send("ok")
            }
        }) { port ->
            val (socket, _) = this.connect(port)
            socket?.sendText("ping", true)?.get(10, TimeUnit.SECONDS)
            Thread.sleep(500)
            println("PROBE_ATTR: " + observed.joinToString(" | "))
            assertEquals(2, observed.size, "expected one connect and one message observation")
            assertEquals(
                "connect attribute=from-upgrade-handler",
                observed[0],
                "attributes set on the upgrade HTTP context are NOT carried to the WsContext"
            )
        }
    }

    @Test
    fun `skipRemainingHandlers in wsBeforeUpgrade rejects the upgrade`() {
        this.withServer({ config ->
            config.routes.wsBeforeUpgrade("/api/ws/run") { ctx ->
                ctx.status(401)
                ctx.skipRemainingHandlers()
            }
        }) { port ->
            val (socket, listener) = this.connect(port)
            println("PROBE_SKIP: socket=${socket != null} closed=${listener.closed} messages=${listener.received}")
            assertNull(socket, "skipRemainingHandlers must abort the upgrade")
        }
    }

    @Test
    fun `without a gate the upgrade succeeds`() {
        this.withServer({}) { port ->
            val (socket, listener) = this.connect(port)
            assertNotNull(socket, "control case: an ungated upgrade must succeed")

            /* And it must be usable, otherwise the rejections above prove nothing. */
            socket!!.sendText("ping", true).get(10, TimeUnit.SECONDS)
            val deadline = System.currentTimeMillis() + 10_000
            while (listener.received.isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(25)
            assertEquals(listOf("hello"), listener.received.toList())
        }
    }
}

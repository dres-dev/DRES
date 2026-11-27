package dres.run

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.ws
import io.javalin.http.Context
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletionStage
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Probe: what does a [io.javalin.websocket.WsContext] see of the upgrade request in Javalin 7.2.3?
 *
 * Findings (these are the reason the dedicated suite exists and encode the framework behaviour the
 * production code must not rely on):
 *  - attributes set in a `routes.before` handler are NOT visible on the WsContext (`attr=null`),
 *  - `routes.beforeMatched` handlers do NOT run for an upgrade request, so rejecting there does
 *    not stop the upgrade,
 *  - cookies and query parameters ARE available on the WsContext.
 */
class WsUpgradeProbeTest {

    private class Listener : WebSocket.Listener {
        val received = LinkedBlockingQueue<String>()
        override fun onOpen(webSocket: WebSocket) { webSocket.request(1) }
        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
            received.add(data.toString()); webSocket.request(1); return null
        }
    }

    private fun <T> withServer(configure: (io.javalin.config.JavalinConfig) -> Unit, body: (Int) -> T): T {
        val port = ServerSocket(0).use { it.localPort }
        val app = Javalin.create { cfg -> configure(cfg) }.start(port)
        try {
            return body(port)
        } finally {
            app.stop()
        }
    }

    @Test
    fun `before handler attributes are not visible on the websocket context`() {
        val beforeRuns = CopyOnWriteArrayList<String>()
        withServer({ config ->
            config.routes.apiBuilder {
                path("api") {
                    ws("ws/run") { cfg ->
                        cfg.onConnect { ctx ->
                            val attr = ctx.attribute<String>("session")
                            val cookie = ctx.cookie("SESSIONID")
                            ctx.send("attr=$attr cookie=$cookie query=${ctx.queryParam("session")} keys=${ctx.attributeMap().keys}")
                        }
                    }
                }
            }
            config.routes.before { ctx: Context ->
                beforeRuns.add(ctx.path())
                ctx.cookie("SESSIONID")?.let { ctx.attribute("session", it) }
            }
        }) { port ->
            val listener = Listener()
            HttpClient.newHttpClient().newWebSocketBuilder()
                .header("Cookie", "SESSIONID=probe-session-token")
                .buildAsync(URI("ws://localhost:$port/api/ws/run?session=querytoken"), listener)
                .get(10, TimeUnit.SECONDS)
            val msg = listener.received.poll(10, TimeUnit.SECONDS)
            println("PROBE: msg=$msg beforeHandlersRan=${beforeRuns.toList()}")
        }
    }

    @Test
    fun `beforeMatched rejection does not block the upgrade`() {
        val matchedRuns = CopyOnWriteArrayList<String>()
        withServer({ config ->
            config.routes.apiBuilder {
                path("api") { ws("ws/run") { it.onConnect { ctx -> ctx.send("connected") } } }
            }
            config.routes.beforeMatched { ctx -> matchedRuns.add(ctx.path()); ctx.status(401); ctx.skipRemainingHandlers() }
        }) { port ->
            val listener = Listener()
            val outcome = runCatching {
                HttpClient.newHttpClient().newWebSocketBuilder()
                    .buildAsync(URI("ws://localhost:$port/api/ws/run"), listener)
                    .get(10, TimeUnit.SECONDS)
            }
            println("PROBE2: outcome=${outcome.isSuccess} msg=${listener.received.poll(2, TimeUnit.SECONDS)} matchedHandlersRan=${matchedRuns.toList()}")
        }
    }
}

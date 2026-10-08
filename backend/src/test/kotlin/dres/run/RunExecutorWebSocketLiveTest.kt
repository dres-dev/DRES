package dres.run

import dev.dres.run.RunExecutor
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.ws
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Live WebSocket test for the run viewer endpoint `/api/ws/run`.
 *
 * Starts a Javalin server with the same route registration as `RestApi` (`path("api") { ws("ws/run", RunExecutor::accept) }`)
 * and connects a real WebSocket client to it. This exercises the Javalin/Jetty upgrade path and the message
 * handling without needing a run manager (PING/REGISTER require one and are covered by [RunExecutorWebSocketTest]).
 */
class RunExecutorWebSocketLiveTest {

    private lateinit var app: Javalin
    private var port: Int = 0
    private val client: HttpClient = HttpClient.newHttpClient()

    /** Client-side listener that records received text frames and the close event. */
    private class Listener : WebSocket.Listener {
        val received = LinkedBlockingQueue<String>()
        val closed = CountDownLatch(1)

        override fun onOpen(webSocket: WebSocket) {
            webSocket.request(1)
        }

        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
            received.add(data.toString())
            webSocket.request(1)
            return null
        }

        override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
            closed.countDown()
            return null
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            closed.countDown()
        }
    }

    @BeforeEach
    fun startServer() {
        port = ServerSocket(0).use { it.localPort }
        app = Javalin.create { config ->
            config.routes.apiBuilder {
                path("api") {
                    ws("ws/run", RunExecutor::accept)
                }
            }
        }.start(port)
    }

    @AfterEach
    fun stopServer() {
        app.stop()
    }

    private fun connect(path: String, listener: Listener = Listener()): WebSocket =
        client.newWebSocketBuilder()
            .buildAsync(URI("ws://localhost:$port$path"), listener)
            .get(10, TimeUnit.SECONDS)

    @Test
    fun `handshake on api ws run is accepted`() {
        val listener = Listener()
        val ws = connect("/api/ws/run", listener)
        assertFalse(ws.isOutputClosed)
        assertEquals(1L, listener.closed.count, "Connection must not be closed by the server after handshake")
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)
    }

    @Test
    fun `handshake on wrong path is rejected`() {
        val listener = Listener()
        assertThrows(ExecutionException::class.java) {
            client.newWebSocketBuilder()
                .buildAsync(URI("ws://localhost:$port/ws/run"), listener)
                .get(10, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `malformed message does not close the connection`() {
        val listener = Listener()
        val ws = connect("/api/ws/run", listener)
        ws.sendText("this is not json", true).get(5, TimeUnit.SECONDS)
        // A well-formed message afterwards must still be processed on the same connection
        ws.sendText("""{"evaluationId":"unknown-run","type":"ACK"}""", true).get(5, TimeUnit.SECONDS)
        assertFalse(listener.closed.await(500, TimeUnit.MILLISECONDS), "Server closed the connection after malformed input")
        assertFalse(ws.isOutputClosed)
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)
    }

    @Test
    fun `message for unknown evaluation gets no reply`() {
        val listener = Listener()
        val ws = connect("/api/ws/run", listener)
        ws.sendText("""{"evaluationId":"unknown-run","type":"PING"}""", true).get(5, TimeUnit.SECONDS)
        assertNull(listener.received.poll(500, TimeUnit.MILLISECONDS), "No run manager exists, so no PING reply is expected")
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS)
    }

    @Test
    fun `client close completes cleanly`() {
        val listener = Listener()
        val ws = connect("/api/ws/run", listener)
        val closing: CompletableFuture<WebSocket> = ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye")
        assertNotNull(closing.get(5, TimeUnit.SECONDS))
        assertTrue(listener.closed.await(5, TimeUnit.SECONDS), "Client did not observe the close handshake")
    }
}

package dres.utilities.concurrent

import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.utilities.concurrent.RenderAwait
import io.javalin.http.Context
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.Future
import java.util.concurrent.TimeoutException

/**
 * Preview handlers waited on rendering futures with no timeout. The comments claimed the wait "should return
 * immediately, since the previews have been prepared", but an external file added during template editing, a
 * range preparation skipped, or a burst of viewers asking for the same uncached segment all produce a wait,
 * and an unbounded one pins a Javalin worker thread. Enough of those and the API stops answering entirely.
 *
 * `RenderAwait` bounds the wait and answers `503` with `Retry-After`; the render itself keeps running and
 * populates the cache, so the retry normally succeeds. These tests pin the three outcomes and the header.
 */
class RenderAwaitTest {

    /**
     * A [Context] that records headers. A Java dynamic proxy is used because `Context` has many members and
     * the classpath has no mocking library; only `header` is expected to be reached.
     */
    private fun recordingContext(headers: MutableMap<String, String> = mutableMapOf()): Context {
        /*
         * `ErrorStatusException` logs `ctx.req().remoteAddr` when it is constructed, so the request also has
         * to be answerable even though the production code never touches it.
         */
        val request = java.lang.reflect.Proxy.newProxyInstance(
            jakarta.servlet.http.HttpServletRequest::class.java.classLoader,
            arrayOf(jakarta.servlet.http.HttpServletRequest::class.java)
        ) { proxy, method, args ->
            when (method.name) {
                "getRemoteAddr" -> "127.0.0.1"
                "getMethod" -> "GET"
                "getRequestURI" -> "/api/v2/test"
                "toString" -> "RecordingRequest"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.getOrNull(0)
                else -> error("Unexpected HttpServletRequest.${method.name}().")
            }
        } as jakarta.servlet.http.HttpServletRequest

        return java.lang.reflect.Proxy.newProxyInstance(
            Context::class.java.classLoader,
            arrayOf(Context::class.java)
        ) { proxy, method, args ->
            when (method.name) {
                "header" -> {
                    headers[args!![0] as String] = args[1] as String
                    null
                }
                "req" -> request
                "path" -> "/api/v2/test"
                "toString" -> "RecordingContext($headers)"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.getOrNull(0)
                else -> error("Unexpected Context.${method.name}(${args?.joinToString() ?: ""}) in a test.")
            }
        } as Context
    }

    private fun future(block: () -> Any?): Future<Any?> = java.util.concurrent.CompletableFuture<Any?>().also { f ->
        Thread {
            try {
                f.complete(block())
            } catch (e: Throwable) {
                f.completeExceptionally(e)
            }
        }.apply { isDaemon = true }.start()
    }

    @Test
    fun `a finished render is returned as is`() {
        val headers = mutableMapOf<String, String>()
        val ctx = recordingContext(headers)
        val value = RenderAwait.await(future { "preview.mp4" }, ctx, "task hint video")

        assertEquals("preview.mp4", value)
        assertTrue(headers.isEmpty(), "A successful wait must not set Retry-After.")
    }

    @Test
    fun `a render that does not finish in time is a 503 with Retry-After`() {
        val headers = mutableMapOf<String, String>()
        val ctx = recordingContext(headers)
        val slow = java.util.concurrent.CompletableFuture<String>() // never completes

        val thrown = assertThrows(ErrorStatusException::class.java) {
            RenderAwait.await(slow, ctx, "task hint video", timeoutMs = 50L)
        }

        assertEquals(503, thrown.statusCode)
        assertTrue(thrown.message!!.contains("still being prepared"), "Message was: ${thrown.message}")
        assertTrue(headers.containsKey("Retry-After"), "A 503 must tell the client when to retry.")
    }

    @Test
    fun `a failed render is a 500 without leaking the cause`() {
        val ctx = recordingContext()
        val failing = future { throw IOException("File /srv/dres/data/secret.mp4 not found") }

        val thrown = assertThrows(ErrorStatusException::class.java) {
            RenderAwait.await(failing, ctx, "task hint video")
        }

        assertEquals(500, thrown.statusCode)
        assertTrue(
            !thrown.message!!.contains("/srv/dres/data"),
            "The response must not carry the internal path: ${thrown.message}"
        )
    }

    /** An interrupted wait must restore the interrupt flag and stop waiting, not swallow it. */
    @Test
    fun `an interrupted wait restores the interrupt flag`() {
        val ctx = recordingContext()
        val pending = java.util.concurrent.CompletableFuture<String>()
        Thread.currentThread().interrupt()
        try {
            val thrown = assertThrows(ErrorStatusException::class.java) {
                RenderAwait.await(pending, ctx, "task hint video")
            }
            assertEquals(503, thrown.statusCode)
            assertTrue(
                Thread.currentThread().isInterrupted,
                "The interrupt flag must stay set so the caller's loop does not spin."
            )
        } finally {
            Thread.interrupted() // clear for the rest of the suite
        }
    }

    /** `TimeoutException` is an implementation detail; callers see an HTTP status. */
    @Test
    fun `a timeout is not surfaced as a raw TimeoutException`() {
        val ctx = recordingContext()
        val timedOut = java.util.concurrent.CompletableFuture<String>().apply {
            completeExceptionally(TimeoutException("internal"))
        }
        assertThrows(ErrorStatusException::class.java) {
            RenderAwait.await(timedOut, ctx, "task hint video")
        }
    }
}

package dev.dres.utilities.concurrent

import dev.dres.api.rest.types.status.ErrorStatusException
import io.javalin.http.Context
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Bounded waiting for a rendering [Future] on a request thread.
 *
 * Preview and hint handlers used to call `future.get()` with no timeout. The comments claimed the wait
 * "should return immediately, since the previews have been prepared", but nothing guarantees that: an
 * external media file introduced while a template is edited, a range that preparation skipped, a burst of
 * viewers asking for the same uncached segment, or a slow FFmpeg job all produce a wait, and an unbounded
 * one pins a Javalin worker thread for as long as the render takes. A handful of those exhausts the
 * request pool and the whole API stops answering - a failure mode that looks like "the server is down".
 *
 * Waiting stays bounded instead: a request that cannot be served in time is told so with `503` and a
 * `Retry-After`, which a client can act on, and the rendering job itself keeps running and populating the
 * cache, so the retry normally succeeds.
 */
object RenderAwait {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    /** Default budget for serving an already-requested render. */
    const val DEFAULT_TIMEOUT_MS: Long = 30_000L

    /** Seconds a client should wait before retrying a render that did not finish in time. */
    private const val RETRY_AFTER_SECONDS = 5

    /**
     * Waits for [future] and returns its value.
     *
     * @param future The rendering job to wait for.
     * @param ctx The request context, used for the error response.
     * @param timeoutMs How long to wait; the job keeps running past this.
     * @param what Short description used in the log and the error, e.g. "task hint video".
     * @return The rendered file.
     * @throws ErrorStatusException 500 if the render failed, 503 if it did not finish in [timeoutMs].
     */
    fun <T> await(future: Future<T>, ctx: Context, what: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): T =
        try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            logger.warn("Giving up waiting for {} after {} ms; the render continues in the background.", what, timeoutMs)
            ctx.header("Retry-After", RETRY_AFTER_SECONDS.toString())
            throw ErrorStatusException(503, "The $what is still being prepared. Retry in $RETRY_AFTER_SECONDS seconds.", ctx)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw ErrorStatusException(503, "Interrupted while waiting for the $what.", ctx)
        } catch (e: ExecutionException) {
            val cause = e.cause ?: e
            if (cause is ErrorStatusException) {
                throw cause
            }
            logger.warn("Rendering the {} failed.", what, cause)
            throw ErrorStatusException(500, "Could not prepare the $what.", ctx)
        }
}

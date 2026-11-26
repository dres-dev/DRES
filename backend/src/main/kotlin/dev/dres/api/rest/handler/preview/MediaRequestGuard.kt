package dev.dres.api.rest.handler.preview

import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Limits how fast a single client may pull media out of DRES and leaves an audit trail for every
 * request.
 *
 * Media endpoints are the only place where a client can retrieve bulk payloads (whole videos,
 * arbitrary preview segments). Without a budget, a single script can walk the API and exfiltrate an
 * entire collection both quickly and without leaving a trace. [MediaRequestGuard] therefore meters
 * requests per session (or, for anonymous callers, per client address) and logs each request to a
 * dedicated `dev.dres.media` logger, so retrieval can be attributed after the fact.
 *
 * This is a throttling measure, not an authorization mechanism: authorization still happens in the
 * respective handlers.
 *
 * @author DRES
 * @version 1.0.0
 */
object MediaRequestGuard {

    /** Logger recording every media request. */
    private val logger = LoggerFactory.getLogger("dev.dres.media")

    /** Marker that allows media logging to be routed or disabled separately. */
    private val marker = MarkerFactory.getMarker("MEDIA")

    /** Number of media requests a single client may issue per [WINDOW_MS]. */
    const val MAX_REQUESTS_PER_WINDOW = 300

    /** Length of the rate limiting window in milliseconds. */
    const val WINDOW_MS = 60_000L

    /** Upper bound for tracked clients, so anonymous traffic cannot grow the map without limit. */
    private const val MAX_TRACKED_CLIENTS = 10_000

    /** Per-client request counter for the current window. */
    private class Window(val start: Long) {
        val count = AtomicInteger(1)
    }

    private val windows = ConcurrentHashMap<String, Window>()

    /**
     * Records the media request described by [ctx] and throws a `429` when the client exceeded
     * [MAX_REQUESTS_PER_WINDOW] requests within [WINDOW_MS].
     *
     * @param ctx The [Context] of the media request.
     * @param kind Short label identifying the endpoint, used in the audit trail.
     * @param subject The media item, file, or other resource that was requested.
     * @throws ErrorStatusException with status 429 if the client is over budget.
     */
    fun enforce(ctx: Context, kind: String, subject: String?) {
        val session = ctx.sessionToken()
        val client = clientOf(session, ctx.req().remoteAddr)
        val now = System.currentTimeMillis()
        val count = this.record(client, now)

        logger.debug(marker, "media request kind={} subject={} session={} client={} count={}", kind, subject, session, client, count)

        if (count > MAX_REQUESTS_PER_WINDOW) {
            /* Only log the rejection once per window, otherwise a blocked client floods the log. */
            if (count == MAX_REQUESTS_PER_WINDOW + 1) {
                logger.warn(marker, "media rate limit exceeded: subject={} session={} client={}", subject, session, client)
            }
            throw ErrorStatusException(429, "Too many media requests. Retry later.", ctx)
        }
    }

    /**
     * Identifies the client a request is metered against. Authenticated callers are metered per
     * session, so logging out and back in does not reset the budget within a window; anonymous
     * callers fall back to their address.
     *
     * @param session The [SessionToken][dev.dres.api.rest.handler.users.SessionToken], if any.
     * @param address The client address, used for anonymous callers.
     * @return The metering key.
     */
    fun clientOf(session: String?, address: String?): String = session ?: "ip:${address ?: "unknown"}"

    /**
     * Records one request by [client] at [now] and returns the number of requests that client made
     * in the current window, including this one.
     *
     * @param client The metering key, see [clientOf].
     * @param now The current time in milliseconds.
     * @return Number of requests in the current window.
     */
    fun record(client: String, now: Long): Int {
        if (this.windows.size > MAX_TRACKED_CLIENTS) {
            this.windows.entries.removeIf { now - it.value.start >= WINDOW_MS }
        }

        val window = this.windows.compute(client) { _, existing ->
            if (existing == null || now - existing.start >= WINDOW_MS) {
                Window(now)
            } else {
                existing.count.incrementAndGet()
                existing
            }
        } ?: Window(now)
        return window.count.get()
    }

    /** Discards all rate limiting state. Intended for tests. */
    fun reset() = this.windows.clear()
}

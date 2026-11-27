package dev.dres.api.rest.handler.preview

import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.util.concurrent.ConcurrentHashMap

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

    /**
     * Per-client sliding window of request timestamps.
     *
     * A fixed window that resets as soon as its start is older than [WINDOW_MS] lets a client send
     * [MAX_REQUESTS_PER_WINDOW] requests at the end of one window and another full budget at the start of the
     * next, i.e. twice the intended rate at the boundary. Keeping the individual timestamps instead makes the
     * budget a genuine "no more than N in any WINDOW_MS" rule.
     *
     * Only `[WINDOW_MS]` worth of timestamps is retained, so the memory per client is bounded by the budget.
     */
    private class Window {
        /** Timestamps of the requests inside the current window, oldest first. */
        val timestamps = java.util.ArrayDeque<Long>(MAX_REQUESTS_PER_WINDOW)

        /** Drops timestamps that have left the window. */
        fun prune(now: Long) {
            while (timestamps.isNotEmpty() && now - timestamps.peekFirst() >= WINDOW_MS) {
                timestamps.removeFirst()
            }
        }
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
        /*
         * Prune expired state BEFORE checking the bound, and evict the least recently active clients when
         * the bound is reached anyway. Previously the map was only trimmed once it was already over the
         * limit, and a rejected client kept its entry, so rotating identifiers (a new session token, or a
         * spoofed forwarded address) could grow the map without limit and turn the bookkeeping itself into a
         * memory-exhaustion vector.
         */
        if (this.windows.size >= MAX_TRACKED_CLIENTS) {
            this.windows.entries.removeIf { entry -> synchronized(entry.value) { entry.value.prune(now); entry.value.timestamps.isEmpty() } }
        }
        while (this.windows.size >= MAX_TRACKED_CLIENTS) {
            /* Still full: drop the oldest-looking entry so a new client can be metered at all. */
            val oldest = this.windows.entries.minByOrNull { entry -> synchronized(entry.value) { entry.value.timestamps.peekFirst() ?: Long.MAX_VALUE } }
                ?: break
            this.windows.remove(oldest.key, oldest.value)
        }

        val window = this.windows.computeIfAbsent(client) { Window() }
        return synchronized(window) {
            window.prune(now)
            window.timestamps.addLast(now)
            window.timestamps.size
        }
    }

    /** Number of clients currently tracked; used by tests to verify the bound. */
    fun trackedClients(): Int = this.windows.size

    /** Discards all rate limiting state. Intended for tests. */
    fun reset() = this.windows.clear()
}

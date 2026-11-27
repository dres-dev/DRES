package dev.dres.api.rest

import io.javalin.http.Context
import org.slf4j.LoggerFactory

/**
 * Access gate for WebSocket upgrades.
 *
 * `routes.before` and `routes.beforeMatched` do **not** run for WebSocket upgrade requests
 * (verified by `backend/src/test/kotlin/dres/run/WsUpgradeProbeTest.kt` and
 * `backend/src/test/kotlin/dres/run/WsUpgradeGateProbeTest.kt`), so neither the session resolution
 * nor the `AccessManager.hasAccess` check ever happens on `/api/ws/run`. Before this gate an
 * anonymous client could therefore hold a socket open indefinitely and only learned from a
 * (previously silent) warning that its REGISTER went nowhere.
 *
 * Unlike `routes.before`, a `wsBeforeUpgrade` handler does run and does receive a full [Context]
 * with cookies and query parameters, and attributes it sets are propagated to the
 * [`io.javalin.websocket.WsContext`] used by the socket handlers. That makes it the right place to
 * resolve the session once for the whole connection.
 *
 * The gate can only check *that* the caller has a session: the evaluation a client is interested in
 * is not known before the first message arrives. Per-evaluation authorisation therefore still
 * happens per message in [`dev.dres.run.RunExecutor`].
 *
 * @author DRES contributors
 */
object WsUpgradeGate {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * Resolves the session of an upgrade request and rejects the connection if it carries none.
     *
     * Accepts the `SESSIONID` cookie (what browsers send automatically on a same-origin handshake)
     * and the `session` query parameter (what client builders that cannot set cookies use).
     *
     * On success the resolved token is stored in the `session` attribute, so the socket handlers
     * resolve it exactly like an HTTP handler would.
     */
    fun gate(ctx: Context) {
        val session = this.sessionOf(ctx)
        if (session == null || AccessManager.rolesOfSession(session).isEmpty()) {
            /* Logged at info: a browser cannot distinguish a rejected handshake from a network
             * failure (both surface as close code 1006), so this line is the only place an operator
             * can see that the socket was refused for a stale or missing session. */
            logger.info(
                "Rejected a WebSocket upgrade from {}: {}.",
                ctx.req().remoteAddr,
                if (session == null) "no session" else "unknown session"
            )
            ctx.status(401)
            ctx.skipRemainingHandlers()
            return
        }
        ctx.attribute("session", session)
    }

    /**
     * Returns the session token sent with an upgrade request, without validating it.
     *
     * A cookie wins over the query parameter, mirroring `RestApi`'s HTTP pipeline.
     */
    fun sessionOf(ctx: Context): String? =
        ctx.cookie(AccessManager.SESSION_COOKIE_NAME) ?: ctx.queryParam("session")
}

package dres.api.rest.types

import dres.api.rest.AccessManager
import dres.mgmt.admin.UserManager
import io.javalin.plugin.json.JavalinJson
import io.javalin.websocket.WsContext
import org.eclipse.jetty.server.session.Session
import java.nio.ByteBuffer

/**
 * Wraps a [WsContext] and gives access to specific information regarding the user owning that [WsContext]
 *
 * @author Ralph Gasser
 * @version 1.0
 */
inline class WebSocketConnection(val context: WsContext) {
    /** ID of the WebSocket session. */
    val sessionId
        get() = context.sessionId

    /** ID of the HTTP session that generated this [WebSocketConnection]. */
    val httpSessionId
        get() = (this.context.session.upgradeRequest.session as Session).id

    /** Name of the user that generated this [WebSocketConnection]. */
    val userName
        get() = UserManager.get(AccessManager.getUserIdForSession(this.httpSessionId) ?: -1L)?.username?.name ?: "UNKNOWN"

    /** IP address of the client. */
    val host
        get() = this.context.host()

    fun send(message: Any) = this.context.send(JavalinJson.toJson(message))
    fun send(message: String) = this.context.send(message)
    fun send(message: ByteBuffer) = this.context.send(message)
}
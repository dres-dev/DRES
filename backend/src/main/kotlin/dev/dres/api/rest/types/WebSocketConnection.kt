package dev.dres.api.rest.types

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.dres.api.rest.AccessManager
import dev.dres.mgmt.admin.DbUserManager
import io.javalin.websocket.WsContext
import org.eclipse.jetty.server.session.Session
import java.nio.ByteBuffer

/**
 * Wraps a [WsContext] and gives access to specific information regarding the user owning that [WsContext]
 *
 * @author Ralph Gasser
 * @version 1.0.1
 */
@JvmInline
value class WebSocketConnection(val context: WsContext) {

    companion object {
        val jsonMapper = jacksonObjectMapper()

        const val UNKNOWN_USER = "UNKNOWN"

    }

    /** ID of the WebSocket session. */
    val sessionId
        get() = context.sessionId

    /** ID of the HTTP session that generated this [WebSocketConnection]. */
    val httpSessionId
        get() = (this.context.session as Session).id

    /** Name of the user that generated this [WebSocketConnection]. */
    val userName
        get() = DbUserManager.get(AccessManager.userIdForSession(this.httpSessionId))?.username ?: UNKNOWN_USER

    /** IP address of the client. */
    val host: String
        get() {
            val xff = this.context.header("X-Forwarded-For")
            return if (xff != null) {
                "$xff via ${this.context.host()}"
            } else {
                this.context.host()
            }

        }

    /**
     * Sends an object through this [WebSocketConnection]. The object is serialized before sending.
     *
     * @param message The message to send.
     */
    fun send(message: Any) = this.context.send(jsonMapper.writeValueAsString(message))

    /**
     * Sends a plain [String] message through this [WebSocketConnection].
     *
     * @param message The [String] to send.
     */
    fun send(message: String) = this.context.send(message)

    /**
     * Sends a plain [ByteBuffer] message through this [WebSocketConnection].
     *
     * @param message The [ByteBuffer] to send.
     */
    fun send(message: ByteBuffer) = this.context.send(message)
}
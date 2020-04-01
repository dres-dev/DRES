package dres.api.websocket

import dres.api.rest.types.run.ServerMessage
import io.javalin.websocket.WsConnectContext
import io.javalin.websocket.WsHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

/**
 * Simple facade that handles all the WebSocket connection.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class WebSocketHandler
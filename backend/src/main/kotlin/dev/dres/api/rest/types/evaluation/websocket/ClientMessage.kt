package dev.dres.api.rest.types.evaluation.websocket

/**
 * Message send by the DRES client via WebSocket to communicate with the DRES server.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class ClientMessage(val runId: String, val type: ClientMessageType)

enum class ClientMessageType {
    ACK,     /** Acknowledgement of the last message received. */
    REGISTER,
    UNREGISTER,
    PING
}
package dev.dres.api.rest.types.evaluation.websocket

/**
 * Enumeration of the types of [ClientMessage]s.
 *
 * @version 1.0.0
 * @author Ralph Gasser
 */
enum class ClientMessageType {
    ACK,     /** Acknowledgement of the last message received. */
    REGISTER,
    UNREGISTER,
    PING
}
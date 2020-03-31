package dres.api.rest.types.run

import kotlinx.serialization.Serializable

/**
 * Message send by the DRES server via WebSocket to inform clients about the state of the run.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
@Serializable
data class ServerMessage(val type: ServerMessageType, val timestamp: Long = System.currentTimeMillis())

enum class ServerMessageType {
    PREPARE,  /** Prepare for run to start. */
    START,    /** Run started. */
    UPDATED,  /** State of run has changed. */
    END       /** Run ended. */
}
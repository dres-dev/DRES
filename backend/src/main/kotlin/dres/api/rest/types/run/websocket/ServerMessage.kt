package dres.api.rest.types.run.websocket

import kotlinx.serialization.Serializable

/**
 * Message send by the DRES server via WebSocket to inform clients about the state of the run.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
@Serializable
data class ServerMessage(val runId: Long, val type: ServerMessageType, val timestamp: Long = System.currentTimeMillis())

enum class ServerMessageType {
    COMPETITION_START, /* Competition run started. */
    COMPETITION_UPDATE, /* State of competition run was updated. */
    COMPETITION_END, /* Competition run ended. */
    TASK_PREPARE,  /** Prepare for a task run to start. */
    TASK_START,    /** Task run started. */
    TASK_UPDATED,  /** State of task run has changed. */
    TASK_END,       /** Taks run ended. */
    PING            /** Keep alive */
}
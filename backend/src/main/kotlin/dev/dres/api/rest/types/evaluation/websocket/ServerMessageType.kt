package dev.dres.api.rest.types.evaluation.websocket

/**
 * Enumeration of the types of [ServerMessage]s.
 *
 * @version 1.0.0
 * @author Ralph Gasser
 */
enum class ServerMessageType {
    COMPETITION_START,      /** Competition run started. */
    COMPETITION_UPDATE,     /** State of competition run was updated. */
    COMPETITION_END,        /** Competition run ended. */
    TASK_PREPARE,           /** Prepare for a task run to start. */
    TASK_START,             /** Task run started. */
    TASK_UPDATED,           /** State of task run has changed; mostly handles arrival of or changes to submissions. */
    TASK_END,               /** Tasks run ended. */
    PING                    /** Keep alive */
}
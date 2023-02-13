package dev.dres.api.rest.types.evaluation.websocket

import dev.dres.data.model.run.interfaces.EvaluationId

/**
 * Message send by the DRES server via WebSocket to inform clients about the state of the run.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
data class ServerMessage(val evaluationId: EvaluationId, val type: ServerMessageType, val timestamp: Long = System.currentTimeMillis())


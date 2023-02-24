package dev.dres.api.rest.types.evaluation.websocket

import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.template.team.TeamId

/**
 * Message send by the DRES server via WebSocket to inform clients about the state of the run.
 *
 * @author Ralph Gasser
 * @version 1.2.0
 */
data class ServerMessage(val evaluationId: EvaluationId, val type: ServerMessageType, val taskId: TaskId? = null, val timestamp: Long = System.currentTimeMillis())


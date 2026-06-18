package dev.dres.api.rest.types.evaluation.websocket

import dev.dres.api.rest.types.evaluation.ApiEvaluationOverview
import dev.dres.api.rest.types.evaluation.ApiEvaluationState
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.run.interfaces.TaskId

/**
 * Message send by the DRES server via WebSocket to inform clients about the state of the run.
 *
 * The optional [state] and [overview] fields carry a state diff so that receivers can update
 * their local representation without issuing a separate HTTP request.
 *
 * @author Ralph Gasser
 * @version 1.3.0
 */
data class ServerMessage(
    val evaluationId: EvaluationId,
    val type: ServerMessageType,
    val taskId: TaskId? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val state: ApiEvaluationState? = null,
    val overview: ApiEvaluationOverview? = null
)


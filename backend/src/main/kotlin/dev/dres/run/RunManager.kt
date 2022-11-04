package dev.dres.run

import dev.dres.api.rest.types.WebSocketConnection
import dev.dres.api.rest.types.evaluation.websocket.ClientMessage
import dev.dres.data.model.template.EvaluationTemplate
import dev.dres.data.model.run.EvaluationId
import dev.dres.data.model.run.InteractiveSynchronousEvaluation
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.RunProperties
import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.validation.interfaces.JudgementValidator

/**
 * A managing class for concrete executions of [EvaluationTemplate], i.e. [InteractiveSynchronousEvaluation]s.
 *
 * @see InteractiveSynchronousEvaluation
 *
 * @author Ralph Gasser
 * @version 1.5.0
 */
interface RunManager : Runnable {
    /** Unique, public [EvaluationId] for this [RunManager]. */
    val id: EvaluationId

    /** A name for identifying this [RunManager]. */
    val name: String

    /** The [EvaluationTemplate] that is executed / run by this [RunManager]. */
    val template: EvaluationTemplate

    /** List of [Scoreboard]s for this [RunManager]. */
    val scoreboards: List<Scoreboard>

    /** Current [RunManagerStatus] of the [RunManager]. */
    val status: RunManagerStatus

    /** [JudgementValidator]s for all tasks that use them */
    val judgementValidators: List<JudgementValidator>

    /** [JudgementValidator]s for all tasks that use them */
    val runProperties: RunProperties

    /**
     * Starts this [RunManager] moving [RunManager.status] from [RunManagerStatus.CREATED] to
     * [RunManagerStatus.ACTIVE]. A [RunManager] can refuse to start.
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @param context The [RunActionContext] for this invocation.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.CREATED]
     */
    fun start(context: RunActionContext)

    /**
     * Ends this [RunManager] moving [RunManager.status] from [RunManagerStatus.ACTIVE] to
     * [RunManagerStatus.TERMINATED]. A [RunManager] can refuse to terminate.
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @param context The [RunActionContext] for this invocation.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    fun end(context: RunActionContext)

    /**
     * Updates the [RunProperties] for this [RunManager].
     *
     * @param properties The new [RunProperties]
     */
    fun updateProperties(properties: RunProperties)

    /**
     * Returns the number of [InteractiveSynchronousEvaluation.Task]s held by this [RunManager].
     *
     * @param context The [RunActionContext] for this invocation.
     * @return The number of [InteractiveSynchronousEvaluation.Task]s held by this [RunManager]
     */
    fun taskCount(context: RunActionContext): Int

    /**
     * Returns a list of all [TaskRun]s that took or are taking place in the scope of this [RunManager].
     *
     * @param context The [RunActionContext] for this invocation.
     * @return List of [TaskRun] that took place (are taking place).
     */
    fun tasks(context: RunActionContext): List<TaskRun>

    /**
     * Returns a list of viewer [WebSocketConnection]s for this [RunManager] alongside with their respective state.
     *
     * @return List of viewer [WebSocketConnection]s for this [RunManager].
     */
    fun viewers(): Map<WebSocketConnection,Boolean>

    /**
     * Invoked by an external caller such in order to inform the [RunManager] that it has received a [ClientMessage].
     *
     * This method does not throw an exception and instead returns false if a [Submission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     * @param connection The [WebSocketConnection] through which the message was received.
     * @param message The [ClientMessage] that was received.
     * @return True if [ClientMessage] was processed, false otherwise
     */
    fun wsMessageReceived(connection: WebSocketConnection, message: ClientMessage): Boolean
}
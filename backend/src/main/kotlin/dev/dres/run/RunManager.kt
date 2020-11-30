package dev.dres.run

import dev.dres.api.rest.types.WebSocketConnection
import dev.dres.api.rest.RestApiRole
import dev.dres.api.rest.types.run.websocket.ClientMessage
import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.run.CompetitionRun
import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.run.score.ScoreTimePoint
import dev.dres.run.score.interfaces.TaskRunScorer
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.updatables.ScoreboardsUpdatable
import dev.dres.run.validation.interfaces.JudgementValidator
import java.util.*

/**
 * A managing class for concrete executions of [CompetitionDescription], i.e. [CompetitionRun]s.
 *
 * @see CompetitionRun
 *
 * @author Ralph Gasser
 * @version 1.4.0
 */
interface RunManager : Runnable {
    /** Unique, public, numeric ID for this [RunManager]. */
    val id: UID


    /** A name for identifying this [RunManager]. */
    val name: String

    /** The [CompetitionDescription] that is executed / run by this [RunManager]. */
    val competitionDescription: CompetitionDescription

    /** The [ScoreboardsUpdatable] used to track the scores per team. */
    val scoreboards: ScoreboardsUpdatable

    /** List of [ScoreTimePoint]s tracking the states of the different [Scoreboard]s over time*/
    val scoreHistory: List<ScoreTimePoint>

    /**
     * Reference to the currently active [TaskDescription].
     *
     * Part of the [RunManager]'s navigational state.
     */
    val currentTask: TaskDescription?

    /**
     * Reference to the [CompetitionRun.TaskRun] that is currently being executed OR that has just ended.
     *
     * Part of the [RunManager]'s execution state. Can be null!
     */
    val currentTaskRun: CompetitionRun.TaskRun?

    /**
     * List of [Submission]s for the current [CompetitionRun.TaskRun].
     *
     * Part of the [RunManager]'s execution state. Can be empty!
     */
    val submissions: List<Submission>

    /** List of all [Submission]s for this [RunManager], irrespective of the [CompetitionRun.TaskRun] it belongs to. */
    val allSubmissions: List<Submission>

    /** Current [RunManagerStatus] of the [RunManager]. */
    val status: RunManagerStatus

    /** [JudgementValidator]s for all tasks that use them */
    val judgementValidators: List<JudgementValidator>

    /**
     * Starts this [RunManager] moving [RunManager.status] from [RunManagerStatus.CREATED] to
     * [RunManagerStatus.ACTIVE]. A [RunManager] can refuse to start.
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.CREATED]
     */
    fun start()

    /**
     * Ends this [RunManager] moving [RunManager.status] from [RunManagerStatus.ACTIVE] to
     * [RunManagerStatus.TERMINATED]. A [RunManager] can refuse to terminate.
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    fun end()

    /**
     * Prepares this [RunManager] for the execution of previous [Task] as per order defined in [CompetitionDescription.tasks].
     * Requires [RunManager.status] to be [RunManagerStatus.ACTIVE].
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @return True if [Task] was moved, false otherwise. Usually happens if last [Task] has been reached.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    fun previousTask(): Boolean

    /**
     * Prepares this [RunManager] for the execution of next [Task] as per order defined in [CompetitionDescription.tasks].
     * Requires [RunManager.status] to be [RunManagerStatus.ACTIVE].
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @return True if [Task] was moved, false otherwise. Usually happens if last [Task] has been reached.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    fun nextTask(): Boolean

    /**
     * Prepares this [RunManager] for the execution of the [Task] given by the index as per order
     * defined in [CompetitionDescription.tasks]. Requires [RunManager.status] to be [RunManagerStatus.ACTIVE].
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    fun goToTask(index: Int)

    /**
     * Starts the [RunManager.currentTask] and thus moves the [RunManager.status] from
     * [RunManagerStatus.ACTIVE] to either [RunManagerStatus.PREPARING_TASK] or [RunManagerStatus.RUNNING_TASK]
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE] or [RunManager.currentTask] is not set.
     */
    fun startTask()

    /**
     * Force-abort the [RunManager.currentTask] and thus moves the [RunManager.status] from
     * [RunManagerStatus.PREPARING_TASK] or [RunManagerStatus.RUNNING_TASK] to [RunManagerStatus.ACTIVE]
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.RUNNING_TASK].
     */
    fun abortTask()

    /**
     * Adjusts the duration of the current [TaskRun] by the specified amount. Amount can be positive or negative.
     *
     * @param s The number of seconds to adjust the duration by.
     * @return Time remaining until the task will end.
     *
     * @throws IllegalArgumentException If the specified correction cannot be applied.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.RUNNING_TASK].
     */
    fun adjustDuration(s: Int): Long

    /**
     * Returns the time in milliseconds that is left until the end of the currently running task.
     * Only works if the [RunManager] is in state [RunManagerStatus.RUNNING_TASK]. If no task is running,
     * this method returns -1L.
     *
     * @return Time remaining until the task will end or -1, if no task is running.
     */
    fun timeLeft(): Long

    /**
     * Returns [CompetitionRun.TaskRun]s for the specified index. The index is zero based, i.e.,
     * an index of 0 returns the first [CompetitionRun.TaskRun], index of 1 the second etc.
     *
     * @param taskRunId The [UID] of the desired [CompetitionRun.TaskRun].
     */
    fun taskRunForId(taskRunId: UID): CompetitionRun.TaskRun?

    /**
     * Returns the number of [CompetitionRun.TaskRun]s held by this [RunManager].
     *
     * @return The number of [CompetitionRun.TaskRun]s held by this [RunManager]
     */
    fun taskRuns(): Int

    /**
     * Returns a list of viewer [WebSocketConnection]s for this [RunManager] alongside with their respective state.
     *
     * @return List of viewer [WebSocketConnection]s for this [RunManager].
     */
    fun viewers(): HashMap<WebSocketConnection,Boolean>

    /**
     * Override the ready state for a given viewer ID.
     *
     * @param viewerId The ID of the viewer that should be overridden.
     * @return true on success, false otherwise
     */
    fun overrideReadyState(viewerId: String): Boolean

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

    /**
     * Invoked by an external caller to post a new [Submission] for the [Task] that is currently being
     * executed by this [RunManager]. [Submission]s usually cause updates to the internal state and/or
     * the [Scoreboard] of this [RunManager].
     *
     * This method will not throw an exception and instead returns false if a [Submission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     * @param sub The [Submission] to be posted.
     * @return [SubmissionStatus] of the [Submission]
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.RUNNING_TASK].
     */
    fun postSubmission(sub: Submission): SubmissionStatus

    /**
     * Invoked by an external caller to update an existing [Submission] by its [Submission.uid] with a new [SubmissionStatus].
     * [Submission]s usually cause updates to the internal state and/or
     * the [Scoreboard] of this [RunManager].
     *
     * This method will not throw an exception and instead returns false if a [Submission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     * @param sub The [Submission] to be posted.
     * @return Whether the update was successfuly or not
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.RUNNING_TASK].
     */
    fun updateSubmission(suid: UID, newStatus: SubmissionStatus): Boolean
}
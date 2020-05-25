package dres.run

import dres.api.rest.types.run.websocket.ClientMessage
import dres.data.model.competition.CompetitionDescription
import dres.data.model.competition.interfaces.TaskDescription
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import dres.data.model.run.TaskRunData
import dres.run.score.interfaces.TaskRunScorer
import dres.run.score.scoreboard.Scoreboard
import dres.run.validation.interfaces.JudgementValidator

/**
 * A managing class for [CompetitionDescription] executions or 'runs'.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
interface RunManager : Runnable {
    /** Unique ID for this [RunManager]. */
    val runId: Long

    /** A name for identifying this [RunManager]. */
    val name: String

    /** The [CompetitionDescription] that is executed / run by this [RunManager]. */
    val competitionDescription: CompetitionDescription

    /** The [Scoreboard] used to track the [Score] per team. */
    val scoreboards: List<Scoreboard>?

    /** The [Task] that is currently being executed or waiting for execution by this [RunManager]. Can be null! */
    val currentTask: TaskDescription?
        get() = currentTaskRun?.task

    val currentTaskRun: TaskRunData?

    /** The [TaskRunScorer] of the current [TaskRun]. Can be null! */
    val currentTaskScore: TaskRunScorer?

    /** The list of [Submission]s for the current [Task]. */
    val submissions: List<Submission>

    /** The list of all [Submission]s, independent of the [Task]. */
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
    fun terminate()

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
     * Returns the time in milliseconds that is left until the end of the currently running [Task].
     * Only works if the [RunManager] is in state [RunManagerStatus.RUNNING_TASK]. If no [Task] is running,
     * this method returns -1L.
     *
     * @return Time that has elapsed since the start of the running [Task] or -1, if no [Task] is running.
     */
    fun timeLeft(): Long

    /**
     * Invoked by an external caller such in order to inform the [RunManager] that it has received a [ClientMessage].
     *
     * This method does not throw an exception and instead returns false if a [Submission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     * @param message The [ClientMessage] that was received.
     * @return True if [ClientMessage] was processed, false otherwise
     */
    fun wsMessageReceived(message: ClientMessage): Boolean

    /**
     *
     */
    fun updateScoreboards()


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
}
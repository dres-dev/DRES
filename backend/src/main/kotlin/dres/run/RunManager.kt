package dres.run

import dres.data.model.competition.Competition
import dres.data.model.competition.Task
import dres.data.model.run.Submission

/**
 * A managing class for [Competition] executions or 'runs'.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
interface RunManager {
    /** The [Competition] that is executed / run by this [RunManager]. */
    val competition: Competition

    /** The [Scoreboard] used to track the [Score] per team. */
    val scoreboard: Scoreboard

    /** The [Task] that is currently being executed or waiting for execution by this [RunManager]. Can be null!*/
    val currentTask: Task?

    /** Current [RunManagerStatus] of the [RunManager]. */
    val status: RunManagerStatus

    /**
     * Starts this [RunManager] moving [RunManager.status] from [RunManagerStatus.CREATED] to
     * [RunManagerStatus.STARTED]. A [RunManager] can refuse to start.
     *
     * @return True if [RunManager] was started, false otherwise.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.CREATED]
     */
    fun start(): Boolean

    /**
     * Ends this [RunManager] moving [RunManager.status] from [RunManagerStatus.STARTED] to
     * [RunManagerStatus.TERMINATED]. A [RunManager] can refuse to terminate.
     *
     * @return True if [RunManager] was terminated, false otherwise.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.STARTED]
     */
    fun terminate()

    /**
     * Prepares this [RunManager] for the execution of next [Task] as per order defined in [Competition.tasks].
     * Requires [RunManager.status] to be [RunManagerStatus.STARTED].
     *
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.STARTED]
     */
    fun nextTask()

    /**
     * Prepares this [RunManager] for the execution of the [Task] given by the index as per order
     * defined in [Competition.tasks]. Requires [RunManager.status] to be [RunManagerStatus.STARTED].
     *
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.STARTED]
     */
    fun goToTask(index: Int)

    /**
     * Starts the [RunManager.currentTask] and thus moves the [RunManager.status] from
     * [RunManagerStatus.STARTED] to either [RunManagerStatus.PREPARING_TASK] or [RunManagerStatus.RUNNING_TASK]
     *
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.STARTED] or [RunManager.currentTask] is not set.
     */
    fun startTask()

    /**
     * Returns the time in milliseconds that has elapsed since the start of the last [Task]. Only works
     * if the [RunManager] is in state [RunManagerStatus.RUNNING_TASK].
     *
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.RUNNING_TASK].
     */
    fun timeElapsed(): Long

    /**
     * Force-abort the [RunManager.currentTask] and thus moves the [RunManager.status] from
     * [RunManagerStatus.PREPARING_TASK] or [RunManagerStatus.RUNNING_TASK] to [RunManagerStatus.STARTED]
     *
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.RUNNING_TASK].
     */
    fun abortTask()

    /**
     * Posts a new [Submission] for the [Task] that is currently being executed by this [RunManager].
     * [Submission]s usually cause updates to the internal state and/or the [Scoreboard] of this [RunManager]
     *
     * @param sub The [Submission] to be posted.
     * @throws IllegalStateException If [RunManager] was not in [RunManagerStatus.RUNNING_TASK]
     */
    fun postSubmission(sub: Submission)
}
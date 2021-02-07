package dev.dres.run

import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.run.CompetitionRun
import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.run.score.scoreboard.Scoreboard

interface InteractiveRunManager : RunManager {

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
     * Override the ready state for a given viewer ID.
     *
     * @param viewerId The ID of the viewer that should be overridden.
     * @return true on success, false otherwise
     */
    fun overrideReadyState(viewerId: String): Boolean

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
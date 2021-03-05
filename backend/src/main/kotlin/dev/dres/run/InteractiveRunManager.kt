package dev.dres.run

import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.run.*
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.run.score.ScoreTimePoint
import dev.dres.run.score.scoreboard.Scoreboard

interface InteractiveRunManager : RunManager {

    /**
     * Reference to the currently active [TaskDescription].
     *
     * Part of the [RunManager]'s navigational state.
     */
    fun currentTask(context: RunActionContext): TaskDescription?

    /**
     * List of [Submission]s for the current [InteractiveSynchronousCompetition.Task].
     *
     * Part of the [RunManager]'s execution state. Can be empty!
     */
    val submissions: List<Submission> //TODO needs to be changed to work with asynchronous runs

    /** List of [ScoreTimePoint]s tracking the states of the different [Scoreboard]s over time*/
    val scoreHistory: List<ScoreTimePoint>

    /** List of all [Submission]s for this [RunManager], irrespective of the [InteractiveSynchronousCompetition.Task] it belongs to. */
    val allSubmissions: List<Submission>

    /**
     * Reference to the [InteractiveSynchronousCompetition.Task] that is currently being executed OR that has just ended.
     *
     * Part of the [RunManager]'s execution state. Can be null!
     */
    val currentTaskRun: InteractiveSynchronousCompetition.Task?

    /**
     *
     */
    override fun tasks(context: RunActionContext): List<AbstractInteractiveTask>

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
    fun previousTask(context: RunActionContext): Boolean

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
    fun nextTask(context: RunActionContext): Boolean

    /**
     * Prepares this [RunManager] for the execution of the [Task] given by the index as per order
     * defined in [CompetitionDescription.tasks]. Requires [RunManager.status] to be [RunManagerStatus.ACTIVE].
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    fun goToTask(context: RunActionContext, index: Int)

    /**
     * Starts the [RunManager.currentTask] and thus moves the [RunManager.status] from
     * [RunManagerStatus.ACTIVE] to either [RunManagerStatus.PREPARING_TASK] or [RunManagerStatus.RUNNING_TASK]
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE] or [RunManager.currentTask] is not set.
     */
    fun startTask(context: RunActionContext)

    /**
     * Force-abort the [RunManager.currentTask] and thus moves the [RunManager.status] from
     * [RunManagerStatus.PREPARING_TASK] or [RunManagerStatus.RUNNING_TASK] to [RunManagerStatus.ACTIVE]
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.RUNNING_TASK].
     */
    fun abortTask(context: RunActionContext)

    /**
     * Adjusts the duration of the current [TaskRun] by the specified amount. Amount can be positive or negative.
     *
     * @param s The number of seconds to adjust the duration by.
     * @return Time remaining until the task will end.
     *
     * @throws IllegalArgumentException If the specified correction cannot be applied.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.RUNNING_TASK].
     */
    fun adjustDuration(context: RunActionContext, s: Int): Long

    /**
     * Returns the time in milliseconds that is left until the end of the currently running task.
     * Only works if the [RunManager] is in state [RunManagerStatus.RUNNING_TASK]. If no task is running,
     * this method returns -1L.
     *
     * @return Time remaining until the task will end or -1, if no task is running.
     */
    fun timeLeft(context: RunActionContext): Long

    /**
     * Returns [InteractiveSynchronousCompetition.Task]s for the specified index. The index is zero based, i.e.,
     * an index of 0 returns the first [InteractiveSynchronousCompetition.Task], index of 1 the second etc.
     *
     * @param taskRunId The [UID] of the desired [InteractiveSynchronousCompetition.Task].
     */
    fun taskRunForId(context: RunActionContext, taskRunId: UID): InteractiveSynchronousCompetition.Task?

    /**
     * Override the ready state for a given viewer ID.
     *
     * @param viewerId The ID of the viewer that should be overridden.
     * @return true on success, false otherwise
     */
    fun overrideReadyState(context: RunActionContext, viewerId: String): Boolean

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
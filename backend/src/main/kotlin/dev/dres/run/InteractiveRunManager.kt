package dev.dres.run

import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.run.*
import dev.dres.data.model.run.interfaces.Task
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.run.score.ScoreTimePoint
import dev.dres.run.score.scoreboard.Scoreboard

interface InteractiveRunManager : RunManager {

    /** List of [ScoreTimePoint]s tracking the states of the different [Scoreboard]s over time*/
    val scoreHistory: List<ScoreTimePoint>

    /** List of all [Submission]s for this [InteractiveRunManager], irrespective of the [InteractiveSynchronousCompetition.Task] it belongs to. */
    val allSubmissions: List<Submission>

    /**
     * Prepares this [InteractiveRunManager] for the execution of previous [TaskDescription] as per order defined in
     * [CompetitionDescription.tasks]. Requires [RunManager.status] to be [RunManagerStatus.ACTIVE].
     *
     * This is part of the [InteractiveRunManager]'s navigational state.  As all state affecting methods, this method throws
     * an [IllegalStateException] if invocation does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return True if [Task] was moved, false otherwise. Usually happens if last [Task] has been reached.
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    fun previous(context: RunActionContext): Boolean

    /**
     * Prepares this [InteractiveRunManager] for the execution of next [TaskDescription] as per order defined in
     * [CompetitionDescription.tasks]. Requires [RunManager.status] to be [RunManagerStatus.ACTIVE].
     *
     * This is part of the [InteractiveRunManager]'s navigational state. As all state affecting methods, this method throws
     * an [IllegalStateException] if invocation oes not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return True if [Task] was moved, false otherwise. Usually happens if last [Task] has been reached.
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    fun next(context: RunActionContext): Boolean

    /**
     * Prepares this [InteractiveRunManager] for the execution of the [TaskDescription] given by the index as per order
     * defined in [CompetitionDescription.tasks]. Requires [RunManager.status] to be [RunManagerStatus.ACTIVE].
     *
     * This is part of the [InteractiveRunManager]'s navigational state. As all state affecting methods, this method throws
     * an [IllegalStateException] if invocation does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @param index The index to navigate to.
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    fun goTo(context: RunActionContext, index: Int)

    /**
     * Reference to the currently active [TaskDescription]. This is part of the [InteractiveRunManager]'s
     * navigational state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return [TaskDescription]
     */
    fun currentTaskDescription(context: RunActionContext): TaskDescription

    /**
     * Starts the [currentTask] and thus moves the [RunManager.status] from
     * [RunManagerStatus.ACTIVE] to either [RunManagerStatus.PREPARING_TASK] or [RunManagerStatus.RUNNING_TASK]
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation oes not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.ACTIVE] or [currentTask] is not set.
     */
    fun startTask(context: RunActionContext)

    /**
     * Force-abort the [currentTask] and thus moves the [RunManager.status] from
     * [RunManagerStatus.PREPARING_TASK] or [RunManagerStatus.RUNNING_TASK] to [RunManagerStatus.ACTIVE]
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.RUNNING_TASK].
     */
    fun abortTask(context: RunActionContext)

    /**
     * Adjusts the duration of the current [AbstractInteractiveTask] by the specified amount. Amount can be positive or negative.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @param s The number of seconds to adjust the duration by.
     * @return Time remaining until the task will end.
     *
     * @throws IllegalArgumentException If the specified correction cannot be applied.
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.RUNNING_TASK].
     */
    fun adjustDuration(context: RunActionContext, s: Int): Long

    /**
     * Returns the time in milliseconds that is left until the end of the currently running task.
     * Only works if the [InteractiveRunManager] is in state [RunManagerStatus.RUNNING_TASK]. If no task is running,
     * this method returns -1L.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return Time remaining until the task will end or -1, if no task is running.
     */
    fun timeLeft(context: RunActionContext): Long

    /**
     * Returns a list of all [AbstractInteractiveTask]s for this [InteractiveRunManager]. Depending on the
     * implementation, that list may be filtered depending on the [RunActionContext].
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return [List] of [AbstractInteractiveTask]s
     */
    override fun tasks(context: RunActionContext): List<AbstractInteractiveTask>

    /**
     * Returns a reference to the currently active [AbstractInteractiveTask].
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return [AbstractInteractiveTask] that is currently active or null, if no such task is active.
     */
    fun currentTask(context: RunActionContext): AbstractInteractiveTask?

    /**
     * Returns [AbstractInteractiveTask]s for the specified index. The index is zero based, i.e., an index of 0 returns the
     * first [AbstractInteractiveTask], index of 1 the second etc.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @param taskId The [UID] of the desired [AbstractInteractiveTask].
     */
    fun taskForId(context: RunActionContext, taskId: UID): AbstractInteractiveTask?

    /**
     * List of [Submission]s for the current [AbstractInteractiveTask].
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return List of [Submission] for the current, [AbstractInteractiveTask]
     */
    fun submissions(context: RunActionContext): List<Submission>

    /**
     * Override the ready state for a given viewer ID.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @param viewerId The ID of the viewer that should be overridden.
     * @return true on success, false otherwise
     */
    fun overrideReadyState(context: RunActionContext, viewerId: String): Boolean

    /**
     * Invoked by an external caller to post a new [Submission] for the [Task] that is currently being
     * executed by this [InteractiveRunManager]. [Submission]s usually cause updates to the internal state and/or
     * the [Scoreboard] of this [InteractiveRunManager].
     *
     * This method will not throw an exception and instead returns false if a [Submission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     *
     * @param context The [RunActionContext] used for the invocation
     * @param sub The [Submission] to be posted.
     *
     * @return [SubmissionStatus] of the [Submission]
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.RUNNING_TASK].
     */
    fun postSubmission(context: RunActionContext, sub: Submission): SubmissionStatus

    /**
     * Invoked by an external caller to update an existing [Submission] by its [Submission.uid] with a new [SubmissionStatus].
     * [Submission]s usually cause updates to the internal state and/or the [Scoreboard] of this [InteractiveRunManager].
     *
     * This method will not throw an exception and instead returns false if a [Submission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     * @param context The [RunActionContext] used for the invocation
     * @param submissionId The [UID] of the [Submission] to update.
     * @param submissionStatus The new [SubmissionStatus]
     *
     * @return Whether the update was successful or not
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.RUNNING_TASK].
     */
    fun updateSubmission(context: RunActionContext, submissionId: UID, submissionStatus: SubmissionStatus): Boolean
}
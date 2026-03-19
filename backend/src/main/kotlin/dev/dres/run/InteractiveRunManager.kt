package dev.dres.run

import dev.dres.api.rest.types.evaluation.submission.ApiVerdictStatus
import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.run.*
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.SubmissionId


interface InteractiveRunManager : RunManager {

    companion object {
        const val COUNTDOWN_DURATION = 5_000 //countdown time in milliseconds
    }

    /**
     * Prepares this [InteractiveRunManager] for the execution of previous [DbTaskTemplate] as per order defined in
     * [DbEvaluationTemplate.tasks]. Requires [RunManager.status] to be [RunManagerStatus.ACTIVE].
     *
     * This is part of the [InteractiveRunManager]'s navigational state.  As all state affecting methods, this method throws
     * an [IllegalStateException] if invocation does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return True if [TaskRun] was moved, false otherwise. Usually happens if last [TaskRun] has been reached.
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    fun previous(context: RunActionContext): Boolean

    /**
     * Prepares this [InteractiveRunManager] for the execution of next [DbTaskTemplate] as per order defined in
     * [DbEvaluationTemplate.tasks]. Requires [RunManager.status] to be [RunManagerStatus.ACTIVE].
     *
     * This is part of the [InteractiveRunManager]'s navigational state. As all state affecting methods, this method throws
     * an [IllegalStateException] if invocation oes not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return True if [TaskRun] was moved, false otherwise. Usually happens if last [TaskRun] has been reached.
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    fun next(context: RunActionContext): Boolean

    /**
     * Prepares this [InteractiveRunManager] for the execution of the [DbTaskTemplate] given by the index as per order
     * defined in [DbEvaluationTemplate.tasks]. Requires [RunManager.status] to be [RunManagerStatus.ACTIVE].
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
     * Reference to the currently active [DbTaskTemplate]. This is part of the [InteractiveRunManager]'s
     * navigational state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return [DbTaskTemplate]
     */
    fun currentTaskTemplate(context: RunActionContext): ApiTaskTemplate

    /**
     * Starts the [currentTask] and thus moves the [RunManager.status] from
     * [RunManagerStatus.ACTIVE] to either [RunManagerStatus.PREPARING_TASK] or [RunManagerStatus.RUNNING_TASK]
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation oes not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.ACTIVE] or [currentTask] is not set.
     */
    fun startTask(context: RunActionContext): TaskId

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
     * @return Time remaining until the task will end or -1, if no task is running. Null if the task runs perpetually.
     */
    fun timeLeft(context: RunActionContext): Long?

    /**
     * Returns the time in milliseconds that has elapsed since the start of the currently running task.
     * Only works if the [InteractiveRunManager] is in state [RunManagerStatus.RUNNING_TASK]. If no task is running,
     * this method returns -1L.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return Time remaining until the task will end or -1, if no task is running.
     */
    fun timeElapsed(context: RunActionContext): Long

    /**
     * Returns a list of all [TaskRun]s for this [InteractiveRunManager]. Depending on the
     * implementation, that list may be filtered depending on the [RunActionContext].
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return [List] of [TaskRun]s
     */
    override fun tasks(context: RunActionContext): List<AbstractInteractiveTask>

    /**
     * Returns a reference to the currently active [TaskRun].
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return [TaskRun] that is currently active or null, if no such task is active.
     */
    fun currentTask(context: RunActionContext): AbstractInteractiveTask?

    /**
     * Returns [TaskRun]s for the specified [EvaluationId].
     *
     * @param context The [RunActionContext] used for the invocation.
     * @param taskId The [EvaluationId] of the desired [TaskRun].
     */
    fun taskForId(context: RunActionContext, taskId: EvaluationId): AbstractInteractiveTask?

    /**
     * Invoked by an external caller to update an existing submission by its [SubmissionId] with a new [ApiVerdictStatus].
     *
     * @param context The [RunActionContext] used for the invocation.
     * @param submissionId The [SubmissionId] of the submission to update.
     * @param submissionStatus The new [ApiVerdictStatus]
     *
     * @return Whether the update was successful or not.
     */
    fun updateSubmission(context: RunActionContext, submissionId: SubmissionId, submissionStatus: ApiVerdictStatus): Boolean

    /**
     * List of all [DbSubmission]s for this [InteractiveRunManager], irrespective of the [DbTask] it belongs to.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return List of [DbSubmission]s
     */
    fun allSubmissions(context: RunActionContext): List<DbSubmission>

    /**
     * List of [DbSubmission]s for the current [DbTask].
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return List of [DbSubmission]s
     */
    fun currentSubmissions(context: RunActionContext): List<DbSubmission>

    /**
     * Override the ready state for a given viewer ID.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @param viewerId The ID of the viewer that should be overridden.
     * @return true on success, false otherwise
     */
    fun overrideReadyState(context: RunActionContext, viewerId: String): Boolean
}

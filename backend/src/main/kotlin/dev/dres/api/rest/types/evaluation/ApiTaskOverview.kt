package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.run.InteractiveRunManager
import kotlinx.serialization.Serializable

/**
 * Overview of a single task run, as shown in run lists.
 *
 * @property timeLeft Seconds remaining until the task ends. `null` means the task has no deadline
 *   (a perpetual task); `-1` means the task is not running. It is computed here, once, so the frontend
 *   does not have to re-derive it and drift from the backend's definition.
 */
@Serializable
data class ApiTaskOverview(
    val id: String,
    val name: String,
    val type: String,
    val group: String,
    val duration: Long?,
    val taskId: String,
    val status: ApiTaskStatus,
    val started: Long?,
    val ended: Long?,
    val timeLeft: Long?) {
    constructor(task: TaskRun) : this(
        task.template.id!!,
        task.template.name,
        task.template.taskGroup,
        task.template.taskType,
        task.template.duration,
        task.taskId,
        task.status,
        task.started,
        task.ended,
        remainingSeconds(task.duration, task.started, task.status, System.currentTimeMillis()))

    companion object {
        /**
         * Remaining time in whole seconds, mirroring
         * [dev.dres.run.InteractiveRunManager.timeLeft] but independent of a manager instance so that
         * an overview can be built from a task run alone.
         *
         * The countdown constant is added because a task's clock starts after a short preparation
         * phase; without it the value reported here would be smaller than the one the viewer shows.
         *
         * @param duration The task's duration in seconds, or null for a perpetual task.
         * @param started When the task run started, in epoch milliseconds.
         * @param status The current task status.
         * @param now The current time, for tests.
         * @return Seconds remaining, `null` for a perpetual task, or `-1` when not running.
         */
        fun remainingSeconds(duration: Long?, started: Long?, status: ApiTaskStatus, now: Long): Long? {
            /* A perpetual task reports no deadline regardless of whether it runs, as the managers do. */
            if (duration == null) {
                return null
            }
            if (status != ApiTaskStatus.RUNNING || started == null) {
                return -1L
            }
            val remainingMs = duration * 1000L - (now - started) + InteractiveRunManager.COUNTDOWN_DURATION
            return Math.max(0L, remainingMs) / 1000L
        }
    }
}

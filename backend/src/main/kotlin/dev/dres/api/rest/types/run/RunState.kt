package dev.dres.api.rest.types.run

import dev.dres.data.model.UID
import dev.dres.data.model.run.InteractiveSynchronousCompetition
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunManager
import dev.dres.run.RunManagerStatus
import dev.dres.run.TaskRunStatus

/**
 * Contains the information about the state of a [InteractiveSynchronousCompetition] and the associated [RunManager].
 *
 * This is information that changes in the course of a run an therefore must be updated frequently.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
data class RunState(val id: String, val status: RestRunManagerStatus, val currentTask: TaskInfo?, val timeLeft: Long, val timeElapsed: Long) {
    constructor(run: InteractiveRunManager, context: RunActionContext) : this(
        run.id.string,
        RestRunManagerStatus.getState(run, context),
        try {
            TaskInfo(run.currentTaskDescription(context))
        } catch (e: Exception) {
            TaskInfo(UID.EMPTY.string, "N/A", "N/A", "N/A", 0)
        },
        run.timeLeft(context) / 1000,
        run.timeElapsed(context) / 1000
    )
}

//FIXME this is only temporary to keep compatibility with the UI
enum class RestRunManagerStatus {
    CREATED,
    ACTIVE,
    PREPARING_TASK,
    RUNNING_TASK,
    TASK_ENDED,
    TERMINATED;

    companion object {
        fun getState(run: InteractiveRunManager, context: RunActionContext) : RestRunManagerStatus {

            return when(run.status) {
                RunManagerStatus.CREATED -> CREATED
                RunManagerStatus.ACTIVE -> {

                    val task = run.currentTask(context) ?: return ACTIVE

                    return when(task.status) {
                        TaskRunStatus.CREATED -> ACTIVE
                        TaskRunStatus.PREPARING -> PREPARING_TASK
                        TaskRunStatus.RUNNING -> RUNNING_TASK
                        TaskRunStatus.ENDED -> TASK_ENDED
                    }

                }
                RunManagerStatus.TERMINATED -> TERMINATED
            }

        }
    }

}
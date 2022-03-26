package dev.dres.api.rest.types.run

import dev.dres.data.model.run.InteractiveSynchronousCompetition
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.*

/**
 * Contains the information about the state of a [InteractiveSynchronousCompetition] and the associated [RunManager].
 *
 * This is information that changes in the course of a run and therefore must be updated frequently.
 *
 * @author Ralph Gasser and Loris Sauter
 * @version 1.1.1
 */
data class RunState(
    val id: String,
    val status: RestRunManagerStatus,
    val currentTask: TaskInfo?,
    val timeLeft: Long,
    val timeElapsed: Long
) {
    constructor(run: InteractiveRunManager, context: RunActionContext) : this(
        run.id.string,
        RestRunManagerStatus.getState(run, context),
        try {
            if (checkAsyncAdmin(run, context)) {
                TaskInfo.EMPTY_INFO
            } else {
                TaskInfo(run.currentTaskDescription(context)) // TODO Loris@26.03 Might be worth to have asyncAdmin versions for these
            }
        } catch (e: Exception) {
            TaskInfo.EMPTY_INFO
        },
        if (checkAsyncAdmin(run, context)) {
            0
        } else {
            run.timeLeft(context) / 1000
        },
        if (checkAsyncAdmin(run, context)) { // TODO Loris@26.03 Might be worth to have asyncAdmin versions for these
            0
        } else {
            run.timeElapsed(context) / 1000 // TODO Loris@26.03 Might be worth to have asyncAdmin versions for these

        }
    )

    companion object {
        /**
         * Checks if the given run is asynchronous and the current user (from the context) is an admin.
         */
        fun checkAsyncAdmin(run: InteractiveRunManager, context: RunActionContext): Boolean {
            return run is InteractiveAsynchronousRunManager && context.isAdmin
        }
    }

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
        fun getState(run: InteractiveRunManager, context: RunActionContext): RestRunManagerStatus {

            return when (run.status) {
                RunManagerStatus.CREATED -> CREATED
                RunManagerStatus.ACTIVE -> {

                    val task = run.currentTask(context) ?: return ACTIVE

                    return when (task.status) {
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

package dev.dres.api.rest.types.run

import dev.dres.data.model.run.InteractiveSynchronousCompetition
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.*

/**
 * Contains the information about the state of a [InteractiveSynchronousCompetition] and the associated [RunManager].
 *
 * This is information that changes in the course of a run and therefore must be updated frequently.
 *
 * @version 1.1.1
 */
data class RunState(
    val id: String,
    val runStatus: RunManagerStatus,
    val taskRunStatus: RestTaskRunStatus,
    val currentTask: TaskInfo?,
    val timeLeft: Long,
    val timeElapsed: Long
) {
    constructor(run: InteractiveRunManager, context: RunActionContext) : this(
        run.id.string,
        //RestRunManagerStatus.getState(run, context),
        run.status,
        RestTaskRunStatus.fromTaskRunStatus(run.currentTask(context)?.status),
        try {
            TaskInfo(run.currentTaskDescription(context))
        } catch (e: IllegalArgumentException) {
            TaskInfo.EMPTY_INFO
        },
        run.timeLeft(context) / 1000,
        run.timeElapsed(context) / 1000
    )

//    companion object {
//        /**
//         * Checks if the given run is asynchronous and the current user (from the context) is an admin.
//         */
//        fun checkAsyncAdmin(run: InteractiveRunManager, context: RunActionContext): Boolean {
//            return run is InteractiveAsynchronousRunManager && context.isAdmin
//        }
//    }

}

enum class RestTaskRunStatus {
    NO_TASK,
    CREATED,
    PREPARING,
    RUNNING,
    ENDED;

    companion object {
        fun fromTaskRunStatus(taskRunStatus: TaskRunStatus?): RestTaskRunStatus = when(taskRunStatus) {
            TaskRunStatus.CREATED -> CREATED
            TaskRunStatus.PREPARING -> PREPARING
            TaskRunStatus.RUNNING -> RUNNING
            TaskRunStatus.ENDED -> ENDED
            null -> NO_TASK
        }
    }
}
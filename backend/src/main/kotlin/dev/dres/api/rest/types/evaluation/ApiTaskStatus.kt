package dev.dres.api.rest.types.evaluation

import dev.dres.run.TaskStatus


/**
 *
 */
enum class ApiTaskStatus(val status: TaskStatus?) {
    NO_TASK(null),
    CREATED(TaskStatus.CREATED),
    PREPARING(TaskStatus.PREPARING),
    RUNNING(TaskStatus.PREPARING),
    ENDED(TaskStatus.ENDED);
}
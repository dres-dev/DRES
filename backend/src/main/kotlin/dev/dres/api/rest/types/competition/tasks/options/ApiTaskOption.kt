package dev.dres.api.rest.types.competition.tasks.options

import dev.dres.data.model.competition.task.options.TaskOption

/**
 * A RESTful API representation of [TaskOption].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiTaskOption(val option: TaskOption) {
    HIDDEN_RESULTS(TaskOption.HIDDEN_RESULTS),
    MAP_TO_SEGMENT(TaskOption.MAP_TO_SEGMENT),
    PROLONG_ON_SUBMISSION(TaskOption.PROLONG_ON_SUBMISSION)
}
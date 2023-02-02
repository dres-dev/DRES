package dev.dres.api.rest.types.competition.tasks.options

import dev.dres.data.model.template.task.options.TaskOption

/**
 * A RESTful API representation of [TaskOption].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiTaskOption {
    HIDDEN_RESULTS, MAP_TO_SEGMENT, PROLONG_ON_SUBMISSION;

    /**
     * Converts this [ApiTaskOption] to a [TaskOption] representation. Requires an ongoing transaction.
     *
     * @return [TaskOption]
     */
    fun toTaskOption(): TaskOption = when(this) {
        HIDDEN_RESULTS -> TaskOption.HIDDEN_RESULTS
        MAP_TO_SEGMENT -> TaskOption.MAP_TO_SEGMENT
        PROLONG_ON_SUBMISSION -> TaskOption.PROLONG_ON_SUBMISSION
    }
}
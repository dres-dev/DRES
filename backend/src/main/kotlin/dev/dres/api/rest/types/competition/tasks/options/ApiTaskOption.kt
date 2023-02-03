package dev.dres.api.rest.types.competition.tasks.options

import dev.dres.data.model.template.task.options.DbTaskOption

/**
 * A RESTful API representation of [DbTaskOption].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiTaskOption {
    HIDDEN_RESULTS, MAP_TO_SEGMENT, PROLONG_ON_SUBMISSION;

    /**
     * Converts this [ApiTaskOption] to a [DbTaskOption] representation. Requires an ongoing transaction.
     *
     * @return [DbTaskOption]
     */
    fun toDb(): DbTaskOption = when(this) {
        HIDDEN_RESULTS -> DbTaskOption.HIDDEN_RESULTS
        MAP_TO_SEGMENT -> DbTaskOption.MAP_TO_SEGMENT
        PROLONG_ON_SUBMISSION -> DbTaskOption.PROLONG_ON_SUBMISSION
    }
}
package dev.dres.api.rest.types.template.tasks.options

import dev.dres.data.model.template.task.options.DbScoreOption

/**
 * A RESTful API representation of [DbScoreOption].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiScoreOption {
    KIS, AVS;

    /**
     * Converts this [ApiScoreOption] to a [DbScoreOption] representation. Requires an ongoing transaction.
     *
     * @return [DbScoreOption]
     */
    fun toDb(): DbScoreOption = when(this) {
        KIS -> DbScoreOption.KIS
        AVS -> DbScoreOption.AVS
    }
}

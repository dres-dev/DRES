package dev.dres.api.rest.types.competition.tasks.options

import dev.dres.data.model.template.task.options.ScoreOption

/**
 * A RESTful API representation of [ScoreOption].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiScoreOption {
    KIS, AVS;

    /**
     * Converts this [ApiScoreOption] to a [ScoreOption] representation. Requires an ongoing transaction.
     *
     * @return [ScoreOption]
     */
    fun toScoreOption(): ScoreOption = when(this) {
        KIS -> ScoreOption.KIS
        AVS -> ScoreOption.AVS
    }
}
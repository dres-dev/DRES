package dev.dres.api.rest.types.competition.tasks.options

import dev.dres.data.model.competition.task.options.ScoreOption

/**
 * A RESTful API representation of [ScoreOption].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiScoreOption(val option: ScoreOption) {
    KIS(ScoreOption.KIS),
    AVS(ScoreOption.AVS)
}
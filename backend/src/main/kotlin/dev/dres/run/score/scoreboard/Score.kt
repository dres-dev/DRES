package dev.dres.run.score.scoreboard

import dev.dres.api.rest.types.evaluation.scores.ApiScore import dev.dres.data.model.template.team.TeamId

/**
 * A container class to track scores per team.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class Score(val teamId: TeamId, val score: Double) {

    /**
     * Converts this [Score] to a RESTful API representation [ApiScore].
     *
     * @return [ApiScore]
     */
    fun toApi(): ApiScore = ApiScore(this.teamId, this.score)
}

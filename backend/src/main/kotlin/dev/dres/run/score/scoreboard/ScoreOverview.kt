package dev.dres.run.score.scoreboard

import dev.dres.api.rest.types.evaluation.scores.ApiScoreOverview

/**
 * A container class to scores for a specific score board.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ScoreOverview(val name: String, val taskGroup: String?, val scores: List<Score>) {

    /**
     * Converts this [ScoreOverview] to a RESTful API representation [ApiScoreOverview].
     *
     * @return [ApiScoreOverview]
     */
    fun toApi() = ApiScoreOverview(this.name, this.taskGroup, this.scores.map { it.toApi() })
}

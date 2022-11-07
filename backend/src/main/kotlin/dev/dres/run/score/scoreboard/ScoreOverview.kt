package dev.dres.run.score.scoreboard

/**
 * A container class to scores for a specific score board.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ScoreOverview(val name: String, val taskGroup: String?, val scores: List<Score>)

package dev.dres.api.rest.types.evaluation.scores

/**
 * A container class to scores for a specific score board.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ApiScoreOverview(val name: String, val taskGroup: String?, val scores: List<ApiScore>)

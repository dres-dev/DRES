package dev.dres.api.rest.types.evaluation.scores

/**
 *
 */
data class ApiScoreSeries(val team: String, val name: String, val points: List<ApiScoreSeriesPoint>)

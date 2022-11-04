package dev.dres.api.rest.types.competition

import dev.dres.data.model.template.EvaluationTemplate

/**
 * An overview over a [EvaluationTemplate].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ApiCompetitionOverview(val id: String, val name: String, val description: String?, val taskCount: Int, val teamCount: Int)
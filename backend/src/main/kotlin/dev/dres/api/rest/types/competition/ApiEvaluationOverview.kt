package dev.dres.api.rest.types.competition

import dev.dres.data.model.template.DbEvaluationTemplate

/**
 * An overview over a [DbEvaluationTemplate].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ApiEvaluationOverview(val id: String, val name: String, val description: String?, val taskCount: Int, val teamCount: Int)
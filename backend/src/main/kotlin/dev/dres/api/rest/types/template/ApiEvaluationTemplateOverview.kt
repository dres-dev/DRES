package dev.dres.api.rest.types.template

import dev.dres.data.model.template.DbEvaluationTemplate

/**
 * An overview over a [DbEvaluationTemplate].
 *
 * @author Ralph Gasser & Loris Sauter
 * @version 1.2.0
 */
data class ApiEvaluationTemplateOverview(val id: String, val name: String, val description: String?, val taskCount: Int, val teamCount: Int)

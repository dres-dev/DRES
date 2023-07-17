package dev.dres.api.rest.types.template

import dev.dres.api.rest.types.evaluation.ApiEvaluationType
import dev.dres.data.model.run.RunProperties
import dev.dres.data.model.template.TemplateId

/**
 * A data class that represents a RESTful request for creating a new [dres.data.model.run.CompetitionRun]
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
data class ApiEvaluationStartMessage(val templateId: TemplateId, val name: String, val type: ApiEvaluationType, val properties: RunProperties = RunProperties())

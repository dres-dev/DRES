package dev.dres.api.rest.types.template

/**
 * A data class that represents a RESTful request for creating a new [dres.data.model.competition.CompetitionDescription]
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class ApiCreateEvaluation(val name: String, val description: String)

package dres.api.rest.types.competition

/**
 * A data class that represents a request for creating a new [Competition][dres.data.model.competition.CompetitionDescription]
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class CompetitionCreateMessage(val name: String, val description: String)
package dev.dres.api.rest.types.competition

import dev.dres.data.model.competition.CompetitionDescription

/**
 * An overview over a [CompetitionDescription].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ApiCompetitionOverview(val id: String, val name: String, val description: String?, val taskCount: Int, val teamCount: Int)
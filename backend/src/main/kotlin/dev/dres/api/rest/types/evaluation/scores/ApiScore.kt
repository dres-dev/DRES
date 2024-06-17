package dev.dres.api.rest.types.evaluation.scores

import dev.dres.data.model.template.team.TeamId
import kotlinx.serialization.Serializable

/**
 * A container class to track scores per team.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class ApiScore(val teamId: TeamId, val score: Double)

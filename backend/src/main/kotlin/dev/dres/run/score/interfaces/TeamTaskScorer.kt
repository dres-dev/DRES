package dev.dres.run.score.interfaces

import dev.dres.data.model.template.team.TeamId

/**
 * A [TaskScorer] that aggregates scores per [TeamId].
 *
 * @author Luca Rossetto
 * @versio 1.0.0
 */
interface TeamTaskScorer : TaskScorer {
    fun teamScoreMap() : Map<TeamId, Double>
}
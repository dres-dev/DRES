package dev.dres.run.score.interfaces

import dev.dres.data.model.UID
import dev.dres.data.model.competition.TeamId
import dev.dres.data.model.run.interfaces.Task

/**
 * A scorer for a [Task]. A score is a [Double] value that captures a teams performance.
 * The [TaskScorer] calculates and tracks these scores per [TeamId].
 *
 * @version 1.1.0
 * @author Luca Rossetto
 */
interface TaskScorer {
    /**
     * Generates and returns the current scores for all teams in the relevant Task.
     *
     * @return Map of team [UID] to team score
     */
    fun scores(): Map<TeamId, Double>
}
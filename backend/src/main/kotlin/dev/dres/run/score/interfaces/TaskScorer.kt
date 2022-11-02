package dev.dres.run.score.interfaces

import dev.dres.data.model.UID
import dev.dres.data.model.competition.team.Team
import dev.dres.data.model.competition.team.TeamId
import dev.dres.data.model.run.interfaces.TaskRun

/** Type alias for a */
typealias ScoreEntry = Triple<TeamId, String?, Double>

/**
 * A scorer for a [TaskRun]. A score is a [Double] value that captures a [Team] performance.
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
    fun scores(): List<ScoreEntry>
}
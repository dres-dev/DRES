package dev.dres.run.score.interfaces

import dev.dres.data.model.UID

/**
 * A scorer for a [TaskRunScorer]
 *
 * @version 1.1.0
 * @author Luca Rossetto
 */
interface TaskRunScorer {
    /**
     * Generates and returns the current scores for all teams in the relevant Task.
     *
     * @return Map of team [UID] to team score
     */
    fun scores(): Map<UID, Double>
}
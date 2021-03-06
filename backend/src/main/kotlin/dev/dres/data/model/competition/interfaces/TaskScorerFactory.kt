package dev.dres.data.model.competition.interfaces

import dev.dres.run.score.interfaces.TaskScorer

/**
 * A factory for [TaskScorer]s.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface TaskScorerFactory {
    /**
     * Generates a new [TaskScorer]. Depending on the implementation, the returned instance
     * is a new instance or being re-use.
     *
     * @return [TaskScorer].
     */
    fun newScorer(): TaskScorer
}
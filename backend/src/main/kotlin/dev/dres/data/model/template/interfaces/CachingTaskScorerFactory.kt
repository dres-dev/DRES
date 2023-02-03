package dev.dres.data.model.template.interfaces

import dev.dres.run.score.scorer.CachingTaskScorer
import dev.dres.run.score.scorer.TaskScorer

/**
 * A factory for [TaskScorer]s.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface CachingTaskScorerFactory {
    /**
     * Generates a new [TaskScorer]. Depending on the implementation, the returned instance
     * is a new instance or being re-use.
     *
     * @return [TaskScorer].
     */
    fun newScorer(): CachingTaskScorer
}
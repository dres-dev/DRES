package dev.dres.run.score.scorer

import dev.dres.data.model.template.team.TeamId
import dev.dres.run.score.Scoreable

/**
 * Non-operational task scorer, which does not calculate a score.
 */
class NoOpTaskScorer(override val scoreable: Scoreable) : TaskScorer {
    override fun scoreMap(): Map<TeamId, Double> = emptyMap()
}

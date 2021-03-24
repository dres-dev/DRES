package dev.dres.data.model.competition.options

import dev.dres.run.score.interfaces.TaskScorer
import dev.dres.run.score.scorer.AvsTaskScorer
import dev.dres.run.score.scorer.KisTaskScorer

/**
 * An [Option] to specify the different types of [TaskScorer]s.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ScoringOption: Option {
    KIS, AVS;

    /**
     * Returns the [TaskScorer] for this [ScoringOption].
     *
     * @param parameters The parameter [Map] used to configure the [TaskScorer]
     */
    fun scorer(parameters: Map<String, String>): TaskScorer = when(this) {
        KIS -> KisTaskScorer(parameters)
        AVS -> AvsTaskScorer()
    }
}
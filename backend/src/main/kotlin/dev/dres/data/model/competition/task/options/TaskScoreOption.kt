package dev.dres.data.model.competition.task.options

import dev.dres.data.model.competition.options.ScoringOption
import dev.dres.run.score.interfaces.TaskScorer
import dev.dres.run.score.scorer.AvsTaskScorer
import dev.dres.run.score.scorer.KisTaskScorer
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * An enumeration of potential options for [TaskDescription] scorers.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class TaskScoreOption(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<TaskScoreOption>() {
        val KIS by enumField { description = "KIS" }
        val AVS by enumField { description = "AVS" }
    }

    /** Name / description of the [TaskScoreOption]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Returns the [TaskScorer] for this [ScoringOption].
     *
     * @param parameters The parameter [Map] used to configure the [TaskScorer]
     */
    fun scorer(parameters: Map<String, String>): TaskScorer = when(this) {
        KIS -> KisTaskScorer(parameters)
        AVS -> AvsTaskScorer()
        else -> throw IllegalStateException("The task score option ${this.description} is currently not supported.")
    }
}
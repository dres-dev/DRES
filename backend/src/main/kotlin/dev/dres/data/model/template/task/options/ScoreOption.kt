package dev.dres.data.model.template.task.options

import dev.dres.api.rest.types.competition.tasks.options.ApiScoreOption
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
class ScoreOption(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<ScoreOption>() {
        val KIS by enumField { description = "KIS" }
        val AVS by enumField { description = "AVS" }
    }

    /** Name / description of the [ScoreOption]. */
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

    /**
     * Converts this [HintOption] to a RESTful API representation [ApiScoreOption].
     *
     * @return [ApiScoreOption]
     */
    fun toApi() = ApiScoreOption.values().find { it.option == this } ?: throw IllegalStateException("Option ${this.description} is not supported.")
}
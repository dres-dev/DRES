package dev.dres.data.model.template.task.options

import dev.dres.api.rest.types.competition.tasks.options.ApiTaskOption
import dev.dres.data.model.template.task.TaskTemplate
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * An enumeration of potential general options for [TaskTemplate].
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class TaskOption(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<TaskOption>() {
        val HIDDEN_RESULTS by enumField { description = "HIDDEN_RESULTS" }               /** Do not show submissions while task is running. */
        val MAP_TO_SEGMENT by enumField { description = "MAP_TO_SEGMENT" }               /** Map the time of a submission to a pre-defined segment. */
        val PROLONG_ON_SUBMISSION by enumField { description = "PROLONG_ON_SUBMISSION" } /** Prolongs a task if a submission arrives within a certain time limit towards the end. */
    }

    /** Name / description of the [ScoreOption]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Converts this [HintOption] to a RESTful API representation [ApiTaskOption].
     *
     * @return [ApiTaskOption]
     */
    fun toApi() = ApiTaskOption.values().find { it.option == this } ?: throw IllegalStateException("Option ${this.description} is not supported.")
}
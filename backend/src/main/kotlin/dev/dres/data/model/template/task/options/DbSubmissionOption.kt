package dev.dres.data.model.template.task.options

import dev.dres.api.rest.types.template.tasks.options.ApiSubmissionOption
import dev.dres.run.filter.*
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * An enumeration of potential options for [TaskDescription] submission settings.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class DbSubmissionOption(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbSubmissionOption>() {
        val NO_DUPLICATES by enumField { description = "NO_DUPLICATES" }
        val LIMIT_CORRECT_PER_TEAM by enumField { description = "LIMIT_CORRECT_PER_TEAM" }
        val LIMIT_WRONG_PER_TEAM by enumField { description = "LIMIT_WRONG_PER_TEAM" }
        val LIMIT_TOTAL_PER_TEAM by enumField { description = "LIMIT_TOTAL_PER_TEAM" }
        val LIMIT_CORRECT_PER_MEMBER by enumField { description = "LIMIT_CORRECT_PER_MEMBER" }
        val TEMPORAL_SUBMISSION by enumField { description = "TEMPORAL_SUBMISSION" }
        val TEXTUAL_SUBMISSION by enumField { description = "TEXTUAL_SUBMISSION" }
        val ITEM_SUBMISSION by enumField { description = "ITEM_SUBMISSION" }
        val MINIMUM_TIME_GAP by enumField { description = "MINIMUM_TIME_GAP" }
    }

    /** Name / description of the [DbScoreOption]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Returns the [SubmissionFilter] for this [SubmissionFilterOption] and the given [parameters].
     *
     * @param parameters The parameter [Map] used to configure the [SubmissionFilter]
     */
    fun newFilter(parameters: Map<String, String>) = when (this) {
        NO_DUPLICATES -> DuplicateSubmissionFilter()
        LIMIT_CORRECT_PER_TEAM -> MaximumCorrectPerTeamFilter(parameters)
        LIMIT_WRONG_PER_TEAM -> MaximumWrongPerTeamFilter(parameters)
        LIMIT_TOTAL_PER_TEAM -> MaximumTotalPerTeamFilter(parameters)
        LIMIT_CORRECT_PER_MEMBER -> MaximumCorrectPerTeamMemberFilter(parameters)
        TEMPORAL_SUBMISSION -> ValidTemporalSubmissionFilter()
        TEXTUAL_SUBMISSION -> ValidTextualSubmissionFilter()
        ITEM_SUBMISSION -> ValidItemSubmissionFilter()
        MINIMUM_TIME_GAP -> SubmissionRateFilter(parameters)
        else -> throw IllegalStateException("The task filter option ${this.description} is currently not supported.")
    }

    /**
     * Converts this [DbHintOption] to a RESTful API representation [ApiSubmissionOption].
     *
     * @return [ApiSubmissionOption]
     */
    fun toApi() = ApiSubmissionOption.values().find { it.toDb() == this } ?: throw IllegalStateException("Option ${this.description} is not supported.")
}

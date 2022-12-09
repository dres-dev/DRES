package dev.dres.data.model.competition.options

import dev.dres.run.filter.*

/**
 * [Option] that can be applied to a task. Configures [SubmissionFilter]s that should be used.
 *
 * @author Luca Rossetto & Ralph Gasser & Loris Sauter
 * @version 1.2.0
 */
enum class SubmissionFilterOption : Option {
    NO_DUPLICATES,
    LIMIT_CORRECT_PER_TEAM,
    LIMIT_WRONG_PER_TEAM,
    LIMIT_TOTAL_PER_TEAM,
    LIMIT_CORRECT_PER_MEMBER,
    LIMIT_CORRECT_PER_ITEM_AND_TEAM,
    TEMPORAL_SUBMISSION,
    TEXTUAL_SUBMISSION,
    ITEM_SUBMISSION,
    MINIMUM_TIME_GAP;

    /**
     * Returns the [SubmissionFilter] for this [SubmissionFilterOption] and the given [parameters].
     *
     * @param parameters The parameter [Map] used to configure the [SubmissionFilter]
     */
    fun filter(parameters: Map<String, String>) = when (this) {
        NO_DUPLICATES -> DuplicateSubmissionFilter()
        LIMIT_CORRECT_PER_TEAM -> CorrectPerTeamFilter(parameters)
        LIMIT_WRONG_PER_TEAM -> MaximumWrongPerTeamFilter(parameters)
        LIMIT_TOTAL_PER_TEAM -> MaximumTotalPerTeamFilter(parameters)
        LIMIT_CORRECT_PER_MEMBER -> CorrectPerTeamMemberFilter(parameters)
        LIMIT_CORRECT_PER_ITEM_AND_TEAM -> CorrectPerTeamItemFilter(parameters)
        TEMPORAL_SUBMISSION -> TemporalSubmissionFilter()
        TEXTUAL_SUBMISSION -> TextualSubmissionFilter()
        ITEM_SUBMISSION -> ItemSubmissionFilter()
        MINIMUM_TIME_GAP -> SubmissionRateFilter(parameters)
    }
}

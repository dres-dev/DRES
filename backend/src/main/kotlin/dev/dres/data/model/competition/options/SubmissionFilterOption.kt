package dev.dres.data.model.competition.options

import dev.dres.run.filter.*

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
enum class SubmissionFilterOption(internal val filter: (parameters: Map<String, String>) -> SubmissionFilter) : Option {
    NO_DUPLICATES({_ -> DuplicateSubmissionFilter() }),
    LIMIT_CORRECT_PER_TEAM({params -> CorrectSubmissionPerTeamFilter(params) }),
    LIMIT_WRONG_PER_TEAM({params ->  MaximumWrongSubmissionsPerTeam(params) }),
    LIMIT_TOTAL_PER_TEAM({params ->  MaximumTotalSubmissionsPerTeam(params) }),
    LIMIT_CORRECT_PER_MEMBER({params -> CorrectSubmissionPerTeamMemberFilter(params) }),
    TEMPORAL_SUBMISSION({_ -> TemporalSubmissionFilter() })
}
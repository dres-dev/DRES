package dev.dres.api.rest.types.competition.tasks.options

import dev.dres.data.model.template.task.options.DbSubmissionOption

/**
 * A RESTful API representation of [DbSubmissionOption].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiSubmissionOption {
    NO_DUPLICATES, LIMIT_CORRECT_PER_TEAM, LIMIT_WRONG_PER_TEAM, LIMIT_TOTAL_PER_TEAM,
    LIMIT_CORRECT_PER_MEMBER, TEMPORAL_SUBMISSION, TEXTUAL_SUBMISSION, ITEM_SUBMISSION, MINIMUM_TIME_GAP;

    /**
     * Converts this [ApiSubmissionOption] to a [DbSubmissionOption] representation. Requires an ongoing transaction.
     *
     * @return [DbSubmissionOption]
     */
    fun toDb(): DbSubmissionOption = when(this) {
        NO_DUPLICATES -> DbSubmissionOption.NO_DUPLICATES
        LIMIT_CORRECT_PER_TEAM -> DbSubmissionOption.LIMIT_CORRECT_PER_TEAM
        LIMIT_WRONG_PER_TEAM -> DbSubmissionOption.LIMIT_WRONG_PER_TEAM
        LIMIT_TOTAL_PER_TEAM -> DbSubmissionOption.LIMIT_TOTAL_PER_TEAM
        LIMIT_CORRECT_PER_MEMBER -> DbSubmissionOption.LIMIT_CORRECT_PER_MEMBER
        TEMPORAL_SUBMISSION -> DbSubmissionOption.TEMPORAL_SUBMISSION
        TEXTUAL_SUBMISSION -> DbSubmissionOption.TEXTUAL_SUBMISSION
        ITEM_SUBMISSION -> DbSubmissionOption.ITEM_SUBMISSION
        MINIMUM_TIME_GAP -> DbSubmissionOption.MINIMUM_TIME_GAP
    }
}
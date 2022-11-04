package dev.dres.api.rest.types.competition.tasks.options

import dev.dres.data.model.template.task.options.SubmissionOption

/**
 * A RESTful API representation of [SubmissionOption].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiSubmissionOption(val option: SubmissionOption) {
    NO_DUPLICATES(SubmissionOption.NO_DUPLICATES),
    LIMIT_CORRECT_PER_TEAM(SubmissionOption.LIMIT_CORRECT_PER_TEAM),
    LIMIT_WRONG_PER_TEAM(SubmissionOption.LIMIT_WRONG_PER_TEAM),
    LIMIT_TOTAL_PER_TEAM(SubmissionOption.LIMIT_TOTAL_PER_TEAM),
    LIMIT_CORRECT_PER_MEMBER(SubmissionOption.LIMIT_CORRECT_PER_MEMBER),
    TEMPORAL_SUBMISSION(SubmissionOption.TEMPORAL_SUBMISSION),
    TEXTUAL_SUBMISSION(SubmissionOption.TEXTUAL_SUBMISSION),
    ITEM_SUBMISSION(SubmissionOption.ITEM_SUBMISSION),
    MINIMUM_TIME_GAP(SubmissionOption.MINIMUM_TIME_GAP)
}
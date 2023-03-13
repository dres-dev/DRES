package dev.dres.run.filter

import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.data.model.submissions.AnswerType
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbAnswerType
import dev.dres.data.model.submissions.Submission
import kotlinx.dnq.query.asSequence

/**
 * A [SubmissionFilter] that filters temporal submissions.
 *
 * @author Luca Rossetto
 * @version 1.1.0
 */
class ItemSubmissionFilter : SubmissionFilter {
    override val reason = "Submission does include temporal information, but whole item was expected"
    override fun test(submission: ApiSubmission): Boolean
        = submission.answerSets().any { it.answers().any { it.type() == AnswerType.ITEM } }
}

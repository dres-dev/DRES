package dev.dres.run.filter

import dev.dres.data.model.submissions.AnswerType
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbAnswerType
import dev.dres.data.model.submissions.Submission
import kotlinx.dnq.query.asSequence


/**
 * A [SubmissionFilter} that checks if a [DbSubmission] contains temporal information.
 *
 * @author Luca Rossetto
 * @version 1.1.0
 */
class TemporalSubmissionFilter : SubmissionFilter {
    override val reason = "Submission does not include temporal information."

    override fun test(submission: Submission): Boolean
        = submission.answerSets().all { set -> set.answers().all {  it.type() == AnswerType.TEMPORAL && it.start != null && it.end != null } } /* TODO: Probably needs adjustment if this is supposed work with batch submissions. */
}
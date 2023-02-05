package dev.dres.run.filter

import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbAnswerType
import dev.dres.data.model.submissions.Submission
import kotlinx.dnq.query.asSequence

/**
 * A [SubmissionFilter} that checks if a [DbSubmission] contains text information.
 *
 * @author Luca Rossetto
 * @version 1.1.0
 */
class TextualSubmissionFilter : SubmissionFilter {

    override val reason = "Submission does not include textual information (or is an empty submission)"

    override fun test(submission: Submission): Boolean
        = submission.answerSets().all { it.answers().all { it.text != null && it.type == DbAnswerType.TEXT } } /* TODO: Probably needs adjustment if this is supposed work with batch submissions. */
}

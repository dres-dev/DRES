package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.VerdictType
import kotlinx.dnq.query.asSequence

/**
 * A [SubmissionFilter] that filters temporal submissions.
 *
 * @author Luca Rossetto
 * @version 1.1.0
 */
class ItemSubmissionFilter : SubmissionFilter {
    override val reason = "Submission does include temporal information, but whole item was expected"
    override fun test(submission: Submission): Boolean
        = submission.verdicts.asSequence().any { it.type == VerdictType.TEMPORAL }
}

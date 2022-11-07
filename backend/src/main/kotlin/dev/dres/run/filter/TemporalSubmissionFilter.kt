package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.VerdictType
import kotlinx.dnq.query.asSequence


/**
 * A [SubmissionFilter} that checks if a [Submission] contains temporal information.
 *
 * @author Luca Rossetto
 * @version 1.1.0
 */
class TemporalSubmissionFilter : SubmissionFilter {
    override val reason = "Submission does not include temporal information."

    override fun test(submission: Submission): Boolean
        = submission.verdicts.asSequence().all {  it.type == VerdictType.TEMPORAL && it.start != null && it.end != null } /* TODO: Probably needs adjustment if this is supposed work with batch submissions. */
}
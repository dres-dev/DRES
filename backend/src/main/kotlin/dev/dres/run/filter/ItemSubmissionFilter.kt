package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.aspects.TemporalSubmissionAspect

class ItemSubmissionFilter : SubmissionFilter {
    override val reason = "Submission does include temporal information, but whole item was expected"

    override fun test(submission: Submission): Boolean = submission !is TemporalSubmissionAspect
}

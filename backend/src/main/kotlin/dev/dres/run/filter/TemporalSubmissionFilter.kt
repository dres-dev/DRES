package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.aspects.TemporalSubmissionAspect


class TemporalSubmissionFilter : SubmissionFilter {
    override fun test(submission: Submission): Boolean = submission is TemporalSubmissionAspect
}
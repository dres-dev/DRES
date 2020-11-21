package dev.dres.run.filter

import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.TemporalSubmissionAspect

class TemporalSubmissionFilter : SubmissionFilter {
    override fun test(submission: Submission): Boolean = submission is TemporalSubmissionAspect

}
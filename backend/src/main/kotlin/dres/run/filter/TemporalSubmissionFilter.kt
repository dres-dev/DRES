package dres.run.filter

import dres.data.model.run.Submission

class TemporalSubmissionFilter : SubmissionFilter {
    override fun test(submission: Submission): Boolean = submission.start != null && submission.end != null

}
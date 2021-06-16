package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission

class SubmissionFilterAggregator(private val filters: List<SubmissionFilter>) : SubmissionFilter {

    override val reason = "" //will never be relevant

    override fun acceptOrThrow(submission: Submission) {
        for (filter in filters) {
            filter.acceptOrThrow(submission)
        }
    }

    override fun test(t: Submission): Boolean = filters.all { it.test(t) }
}
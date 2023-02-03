package dev.dres.run.filter

import dev.dres.data.model.submissions.DbSubmission

class SubmissionFilterAggregator(private val filters: List<SubmissionFilter>) : SubmissionFilter {

    override val reason = "" //will never be relevant

    override fun acceptOrThrow(submission: DbSubmission) {
        for (filter in filters) {
            filter.acceptOrThrow(submission)
        }
    }

    override fun test(t: DbSubmission): Boolean = filters.all { it.test(t) }
}
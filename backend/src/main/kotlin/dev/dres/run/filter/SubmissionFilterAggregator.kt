package dev.dres.run.filter

import dev.dres.api.rest.types.evaluation.ApiSubmission

class SubmissionFilterAggregator(private val filters: List<SubmissionFilter>) : SubmissionFilter {

    override val reason = "" //will never be relevant

    override fun acceptOrThrow(submission: ApiSubmission) {
        for (filter in filters) {
            filter.acceptOrThrow(submission)
        }
    }

    override fun test(t: ApiSubmission): Boolean = filters.all { it.test(t) }
}
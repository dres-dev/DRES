package dev.dres.run.filter.basics

import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.data.model.run.AbstractTask
import dev.dres.run.filter.SubmissionRejectedException

/**
 * A [SubmissionFilter] that combines multiple [SubmissionFilter]s.
 *
 * @author Luca Rossetto
 * @version 2.0.0
 */
class CombiningSubmissionFilter(private val filters: List<SubmissionFilter>) : SubmissionFilter {

    /**
     * Tests the given [ApiClientSubmission] for the provided [AbstractTask] with this [SubmissionFilter] and throws a [SubmissionRejectedException], if the test fails.
     *
     * @param submission The [ApiClientSubmission] to check.
     * @throws SubmissionRejectedException on failure
     */
    override fun acceptOrThrow(submission: ApiClientSubmission) {
        for (filter in filters) {
            filter.acceptOrThrow(submission)
        }
    }

    /**
     * Tests the given [ApiClientSubmission] with this [SubmissionFilter] return true, if test succeeds.
     *
     * @param t The [ApiClientSubmission] to check.
     * @return True on success, false otherwise.
     */
    override fun test(t: ApiClientSubmission): Boolean = this.filters.all { it.test(t) }
}
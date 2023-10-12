package dev.dres.run.filter.basics

import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.run.filter.SubmissionRejectedException

/**
 * A [SubmissionFilter] that lets no [ApiClientSubmission] pass.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object AcceptNoneSubmissionFilter: SubmissionFilter {
    /**
     * Tests the given [ApiClientSubmission] with this [AcceptAllSubmissionFilter].
     *
     * @param submission The [ApiClientSubmission] to check.
     */
    override fun acceptOrThrow(submission: ApiClientSubmission) {
        throw SubmissionRejectedException(submission, "No submission are accepted!")
    }

    /**
     * Evaluates this [SubmissionFilter] on the given argument. Always returns true.
     *
     * @param submission The input argument.
     * @return True
     */
    override fun test(submission: ApiClientSubmission): Boolean = false
}

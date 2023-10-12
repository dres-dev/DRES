package dev.dres.run.filter.basics

import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.data.model.run.AbstractTask
import dev.dres.data.model.submissions.Submission
import dev.dres.run.filter.SubmissionRejectedException
import java.util.function.Predicate

/**
 * A [Predicate] that can be used to filter [Submission]'s prior to them being processed
 * by the [Submission] evaluation pipeline.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
interface SubmissionFilter : Predicate<ApiClientSubmission> {
    /**
     * Tests the given [ApiClientSubmission] for the provided [AbstractTask] with this [SubmissionFilter] and throws a [SubmissionRejectedException], if the test fails.
     *
     * @param submission The [ApiClientSubmission] to check.
     * @throws SubmissionRejectedException on failure
     */
    fun acceptOrThrow(submission: ApiClientSubmission)
}
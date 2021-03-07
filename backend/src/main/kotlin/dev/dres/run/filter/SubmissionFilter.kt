package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission
import java.util.function.Predicate

/**
 * A [Predicate] that can be used to filter [Submission]'s prior to them being processed
 * by the [Submission] evaluation pipeline.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
fun interface SubmissionFilter : Predicate<Submission> {
    override infix fun and(other: Predicate<in Submission>): SubmissionFilter = SubmissionFilter { s -> this@SubmissionFilter.test(s) && other.test(s) }
    override infix fun or(other: Predicate<in Submission>): SubmissionFilter = SubmissionFilter { s -> this@SubmissionFilter.test(s) || other.test(s) }
    operator fun not(): SubmissionFilter = SubmissionFilter { s -> !this@SubmissionFilter.test(s) }

    /**
     * Tests the given [Submission] with this [SubmissionFilter] and throws a [SubmissionRejectedException], if the test fails.
     *
     * @param submission The [Submission] to check.
     * @throws SubmissionRejectedException on failure
     */
    fun acceptOrThrow(submission: Submission) {
        if (!this.test(submission)) {
            throw SubmissionRejectedException(submission)
        }
    }
}
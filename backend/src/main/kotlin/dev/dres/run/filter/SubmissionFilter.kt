package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission
import org.slf4j.LoggerFactory
import java.util.function.Predicate

/**
 * A [Predicate] that can be used to filter [Submission]'s prior to them being processed
 * by the [Submission] evaluation pipeline.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
interface SubmissionFilter : Predicate<Submission> {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    val reason: String

    /**
     * Tests the given [Submission] with this [SubmissionFilter] and throws a [SubmissionRejectedException], if the test fails.
     *
     * @param submission The [Submission] to check.
     * @throws SubmissionRejectedException on failure
     */
    fun acceptOrThrow(submission: Submission) {
        if (!this.test(submission)) {
            LOGGER.info("Submission $${submission.submissionId} was rejected by filter: $reason")
            throw SubmissionRejectedException(submission, reason)
        }
    }
}
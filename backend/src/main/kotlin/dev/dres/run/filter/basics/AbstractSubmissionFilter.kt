package dev.dres.run.filter.basics

import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.data.model.run.AbstractTask
import dev.dres.run.filter.SubmissionRejectedException
import org.slf4j.LoggerFactory

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
abstract class AbstractSubmissionFilter(private val reason: String): SubmissionFilter {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(SubmissionFilter::class.java)
    }

    /**
     * Tests the given [ApiClientSubmission] for the provided [AbstractTask] with this [SubmissionFilter] and throws a [SubmissionRejectedException], if the test fails.
     *
     * @param submission The [ApiClientSubmission] to check.
     * @throws SubmissionRejectedException on failure
     */
    final override fun acceptOrThrow(submission: ApiClientSubmission) {
        if (!this.test(submission)) {
            LOGGER.info("Submission ${submission.submissionId} was rejected by filter: $reason")
            throw SubmissionRejectedException(submission, this.reason)
        }
    }
}
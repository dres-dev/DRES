package dev.dres.run.filter

import dev.dres.data.model.submissions.DbSubmission
import org.slf4j.LoggerFactory
import java.util.function.Predicate

/**
 * A [Predicate] that can be used to filter [DbSubmission]'s prior to them being processed
 * by the [DbSubmission] evaluation pipeline.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
interface SubmissionFilter : Predicate<DbSubmission> {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    val reason: String

    /**
     * Tests the given [DbSubmission] with this [SubmissionFilter] and throws a [SubmissionRejectedException], if the test fails.
     *
     * @param submission The [DbSubmission] to check.
     * @throws SubmissionRejectedException on failure
     */
    fun acceptOrThrow(submission: DbSubmission) {
        if (!this.test(submission)) {
            LOGGER.info("Submission $${submission.submissionId} was rejected by filter: $reason")
            throw SubmissionRejectedException(submission, reason)
        }
    }
}
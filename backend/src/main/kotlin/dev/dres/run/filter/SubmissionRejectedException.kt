package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission
import org.slf4j.LoggerFactory

/**
 * An exception that is thrown, when a [Submission] is rejected by a [SubmissionFilter].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class SubmissionRejectedException(s: Submission, reason: String) : Throwable("Submission $s was rejected by filter: $reason") {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    init {
        logger.info("Submission $s was rejected by filter: $reason")
    }

}
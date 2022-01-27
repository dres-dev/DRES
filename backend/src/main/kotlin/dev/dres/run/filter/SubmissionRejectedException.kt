package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.aspects.ItemAspect
import dev.dres.data.model.submissions.aspects.TextAspect
import org.slf4j.LoggerFactory

/**
 * An exception that is thrown, when a [Submission] is rejected by a [SubmissionFilter].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class SubmissionRejectedException(s: Submission, reason: String) : Throwable(
    "Submission ${
        when (s) {
            is ItemAspect -> "for item ${s.item.name}"
            is TextAspect -> "with text '${s.text}'"
            else -> ""
        }
    } was rejected by filter: $reason"
) {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    init {
        logger.info("Submission $s was rejected by filter: $reason")
    }

}
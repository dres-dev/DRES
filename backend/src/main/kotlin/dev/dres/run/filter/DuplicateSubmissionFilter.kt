package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.isEmpty


/**
 * A [SubmissionFilter] that filters duplicate [Submission]s in terms of content.
 *
 * @author Luca Rossetto
 * @version 1.1.0
 */
class DuplicateSubmissionFilter : SubmissionFilter {

    override val reason = "Duplicate submission received."

    override fun test(submission: Submission): Boolean {
        return submission.verdicts.asSequence().all { verdict ->
            verdict.task.submissions.filter {
                (it.text eq verdict.text) and (it.item eq verdict.item) and (it.start le (verdict.start ?: Long.MAX_VALUE)) and (it.end ge (verdict.end ?: Long.MIN_VALUE))
            }.isEmpty
        }
    }
}
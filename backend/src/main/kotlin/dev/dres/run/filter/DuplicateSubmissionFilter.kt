package dev.dres.run.filter

import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.Submission
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.isEmpty


/**
 * A [SubmissionFilter] that filters duplicate [DbSubmission]s in terms of content.
 *
 * @author Luca Rossetto
 * @version 1.1.0
 */
class DuplicateSubmissionFilter : SubmissionFilter {

    override val reason = "Duplicate submission received."

    override fun test(submission: ApiSubmission): Boolean { //TODO semantics unclear
//        return submission.answerSets.asSequence().all { verdict ->
//            verdict.task.submissions.filter {set ->
//                set.answers.filter {
//                    (it.text eq verdict.text) and (it.item eq verdict.item) and (it.start le (verdict.start
//                        ?: Long.MAX_VALUE)) and (it.end ge (verdict.end ?: Long.MIN_VALUE))
//                }
//            }.isEmpty
//        }

        return true
    }
}
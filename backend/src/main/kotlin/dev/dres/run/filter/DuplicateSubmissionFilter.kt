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

    override fun test(submission: ApiSubmission): Boolean =

        submission.answers.groupBy { it.taskId }.all {

            val task = it.value.firstOrNull()?.task() ?: return@all true

            val presentSubmissions = task.answerSets().filter { it.submission.teamId == submission.teamId }

            presentSubmissions.forEach { presentAnswerSet ->
                if (it.value.any { it equivalent presentAnswerSet }) { //any overlap in answerSets
                    return@all false
                }
            }

            true
        }


}
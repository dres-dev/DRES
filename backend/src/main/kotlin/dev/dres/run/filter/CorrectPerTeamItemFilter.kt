package dev.dres.run.filter

import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.VerdictStatus
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.size


class CorrectPerTeamItemFilter(private val limit: Int = 1) : SubmissionFilter {

    constructor(parameters: Map<String, String>) : this(parameters.getOrDefault("limit", "1").toIntOrNull() ?: 1)

    override val reason: String = "Maximum number of correct submissions ($limit) exceeded for this item."

    override fun test(submission: Submission): Boolean {
        val submittedItems = submission.answerSets().flatMap { it.answers() }.mapNotNull { it.item }.toSet()
        return submission.answerSets().all { answerSet ->
            answerSet.task.answerSets().filter { taskAnswerSets ->
                (taskAnswerSets.status() == VerdictStatus.CORRECT) && taskAnswerSets.submission.team == submission.team && taskAnswerSets.answers().any { it.item in submittedItems }
            }.count() < this.limit
        }
    }

}
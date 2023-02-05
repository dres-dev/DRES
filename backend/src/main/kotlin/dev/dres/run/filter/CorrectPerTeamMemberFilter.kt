package dev.dres.run.filter

import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.VerdictStatus
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.size

/**
 * A [SubmissionFilter] that filters correct [DbSubmission]s if the number of correct [DbSubmission] for the team member exceeds the limit.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @version 1.1.0
 */
class CorrectPerTeamMemberFilter(private val limit: Int = 1) : SubmissionFilter {
    constructor(parameters: Map<String, String>) : this(parameters.getOrDefault("limit", "1").toIntOrNull() ?: 1)

    override val reason = "Maximum number of correct submissions ($limit) exceeded for the team member."
    override fun test(submission: Submission): Boolean {
        return submission.answerSets().all { answer ->
            answer.task.answerSets().filter {
                (it.status eq VerdictStatus.Status.CORRECT) && it.submission.team == submission.team && it.submission.user == submission.user
            }.count() < limit
        }
    }
}
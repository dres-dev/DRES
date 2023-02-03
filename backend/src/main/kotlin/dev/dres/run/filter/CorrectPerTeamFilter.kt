package dev.dres.run.filter

import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbVerdictStatus
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.size

/**
 * A [SubmissionFilter] that filters correct [DbSubmission]s if the number of correct [DbSubmission] for the team exceed the limit.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @version 1.1.0
 */
class CorrectPerTeamFilter(private val limit: Int = 1) : SubmissionFilter {
    override val reason = "Maximum number of correct submissions ($limit) exceeded for the team."

    constructor(parameters: Map<String, String>) : this(parameters.getOrDefault("limit", "1").toIntOrNull() ?: 1)
    override fun test(submission: DbSubmission): Boolean {
        return submission.answerSets.asSequence().all { verdict ->
            verdict.task.submissions.filter { (it.status eq DbVerdictStatus.CORRECT).and(it.submission.team eq submission.team) }.size() < limit
        }
    }
}
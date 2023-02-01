package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.VerdictStatus
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.size


class CorrectPerTeamItemFilter(private val limit: Int = 1) : SubmissionFilter {

    constructor(parameters: Map<String, String>) : this(parameters.getOrDefault("limit", "1").toIntOrNull() ?: 1)

    override val reason: String = "Maximum number of correct submissions ($limit) exceeded for this item."

    override fun test(submission: Submission): Boolean {
        return submission.verdicts.asSequence().all { verdict ->
            verdict.task.submissions.filter {
                (it.status eq VerdictStatus.CORRECT).and(it.submission.team eq submission.team)
            }.size() < this.limit
        }
    }

}
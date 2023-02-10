package dev.dres.run.filter

import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.template.task.options.DbSubmissionOption
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.size

class MaximumTotalPerTeamFilter(private val max: Int = Int.MAX_VALUE) : SubmissionFilter {

    companion object {
        val PARAMETER_KEY_LIMIT = "${DbSubmissionOption.LIMIT_TOTAL_PER_TEAM.description}.limit"
    }

    constructor(parameters: Map<String, String>) : this(parameters.getOrDefault(PARAMETER_KEY_LIMIT, "${Int.MAX_VALUE}").toIntOrNull() ?: Int.MAX_VALUE)

    override val reason = "Maximum total number of submissions ($max) exceeded for the team"

    /**
     * TODO: This filter now takes all [Verdict]s into account. Is this desired behaviour?
     */
    override fun test(submission: Submission): Boolean {
        return submission.answerSets().all { answerSet ->
            answerSet.task.answerSets().filter { it.submission.teamId == submission.teamId }.count() < max
        }
    }
}


package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.VerdictStatus
import kotlinx.dnq.query.FilteringContext.eq
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.size

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class MaximumWrongPerTeamFilter(private val max: Int = Int.MAX_VALUE) : SubmissionFilter {
    constructor(parameters: Map<String, String>) : this(parameters.getOrDefault("limit", "${Int.MAX_VALUE}").toIntOrNull() ?: Int.MAX_VALUE)

    override val reason = "Maximum number of wrong submissions ($max) exceeded for the team"
    /**
     * TODO: This filter now takes all [Verdict]s into account. Is this desired behaviour?
     */
    override fun test(submission: Submission): Boolean {
        return submission.verdicts.asSequence().all { v ->
            v.task.submissions.filter { (it.submission.team.id eq submission.team.id) and (it.status eq VerdictStatus.WRONG) }.size() < max
        }
    }
}
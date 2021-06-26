package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission

class MaximumTotalPerTeamFilter(private val max: Int = Int.MAX_VALUE) : SubmissionFilter {
    constructor(parameters: Map<String, String>) : this(parameters.getOrDefault("limit", "${Int.MAX_VALUE}").toIntOrNull() ?: Int.MAX_VALUE)

    override val reason = "Maximum total number of submissions ($max) exceeded for the team"

    override fun test(submission: Submission): Boolean = submission.task!!.submissions.filter { it.teamId == submission.teamId }.size < max
}


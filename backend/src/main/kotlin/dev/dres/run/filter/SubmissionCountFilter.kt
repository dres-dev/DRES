package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus

class MaximumTotalSubmissionsPerTeam(private val max: Int = Int.MAX_VALUE) : SubmissionFilter {
    constructor(parameters: Map<String, String>) : this(parameters.getOrDefault("limit", "${Int.MAX_VALUE}").toIntOrNull() ?: Int.MAX_VALUE)
    override fun test(submission: Submission): Boolean = submission.task!!.submissions.filter { it.teamId == submission.teamId }.size < max
}

class MaximumWrongSubmissionsPerTeam(private val max: Int = Int.MAX_VALUE) : SubmissionFilter {
    constructor(parameters: Map<String, String>) : this(parameters.getOrDefault("limit", "${Int.MAX_VALUE}").toIntOrNull() ?: Int.MAX_VALUE)
    override fun test(submission: Submission): Boolean = submission.task!!.submissions.filter { it.teamId == submission.teamId && it.status == SubmissionStatus.WRONG }.size < max
}
package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus

class CorrectSubmissionPerTeamFilter(private val limit: Int = 1) : SubmissionFilter {
    constructor(parameters: Map<String, String>) : this(parameters.getOrDefault("limit", "1").toIntOrNull() ?: 1)
    override fun test(submission: Submission): Boolean = submission.task!!.submissions.count { it.status == SubmissionStatus.CORRECT && it.teamId == submission.teamId } < limit
}

class CorrectSubmissionPerTeamMemberFilter(private val limit: Int = 1) : SubmissionFilter {
    constructor(parameters: Map<String, String>) : this(parameters.getOrDefault("limit", "1").toIntOrNull() ?: 1)
    override fun test(submission: Submission): Boolean = submission.task!!.submissions.count { it.status == SubmissionStatus.CORRECT && it.teamId == submission.teamId && it.memberId == submission.memberId } < limit
}
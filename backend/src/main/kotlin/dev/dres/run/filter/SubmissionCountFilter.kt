package dev.dres.run.filter

import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.SubmissionStatus

class MaximumTotalSubmissionsPerTeam(private val max: Int) : SubmissionFilter {
    override fun test(submission: Submission): Boolean = submission.taskRun!!.submissions.filter { it.team == submission.team }.size < max
}

class MaximumWrongSubmissionsPerTeam(private val max: Int) : SubmissionFilter {
    override fun test(submission: Submission): Boolean = submission.taskRun!!.submissions.filter { it.team == submission.team && it.status == SubmissionStatus.WRONG }.size < max
}
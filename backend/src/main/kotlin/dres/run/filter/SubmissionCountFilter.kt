package dres.run.filter

import dres.data.model.run.CompetitionRun
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus

class MaximumTotalSubmissionsPerTeam(private val taskRun: CompetitionRun.TaskRun, private val max: Int) : SubmissionFilter {
    override fun test(submission: Submission): Boolean = taskRun.submissions.filter { it.team == submission.team }.size < max
}

class MaximumWrongSubmissionsPerTeam(private val taskRun: CompetitionRun.TaskRun, private val max: Int) : SubmissionFilter {
    override fun test(submission: Submission): Boolean = taskRun.submissions.filter { it.team == submission.team && it.status == SubmissionStatus.WRONG }.size < max
}
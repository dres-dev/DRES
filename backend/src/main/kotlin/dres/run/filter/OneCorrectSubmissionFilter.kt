package dres.run.filter

import dres.data.model.run.CompetitionRun
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus

class OneCorrectSubmissionPerTeamFilter(private val taskRun: CompetitionRun.TaskRun) : SubmissionFilter {
    override fun test(submission: Submission): Boolean = taskRun.submissions.none { it.status == SubmissionStatus.CORRECT && it.team == submission.team }
}

class OneCorrectSubmissionPerTeamToolFilter(private val taskRun: CompetitionRun.TaskRun) : SubmissionFilter {
    override fun test(submission: Submission): Boolean = taskRun.submissions.none { it.status == SubmissionStatus.CORRECT && it.team == submission.team && it.tool == submission.tool }
}
package dres.run.filter

import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus

class OneCorrectSubmissionPerTeamFilter : SubmissionFilter {
    override fun test(submission: Submission): Boolean = submission.taskRun!!.submissions.none { it.status == SubmissionStatus.CORRECT && it.team == submission.team }
}

class OneCorrectSubmissionPerTeamToolFilter : SubmissionFilter {
    override fun test(submission: Submission): Boolean = submission.taskRun!!.submissions.none { it.status == SubmissionStatus.CORRECT && it.team == submission.team && it.member == submission.member }
}
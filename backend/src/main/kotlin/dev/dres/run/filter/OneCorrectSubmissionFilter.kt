package dev.dres.run.filter

import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.SubmissionStatus

class OneCorrectSubmissionPerTeamFilter : SubmissionFilter {
    override fun test(submission: Submission): Boolean = submission.taskRun!!.submissions.none { it.status == SubmissionStatus.CORRECT && it.teamId == submission.teamId }
}

class OneCorrectSubmissionPerTeamToolFilter : SubmissionFilter {
    override fun test(submission: Submission): Boolean = submission.taskRun!!.submissions.none { it.status == SubmissionStatus.CORRECT && it.teamId == submission.teamId && it.memberId == submission.memberId }
}
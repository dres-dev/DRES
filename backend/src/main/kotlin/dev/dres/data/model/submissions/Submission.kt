package dev.dres.data.model.submissions

import dev.dres.data.model.admin.UserId
import dev.dres.data.model.template.team.TeamId

typealias SubmissionId = String

interface Submission {

    val submissionId: SubmissionId
    val timestamp: Long
    val teamId: TeamId
    val memberId: UserId

    fun answerSets(): Sequence<AnswerSet>

    fun toDb(): DbSubmission

}
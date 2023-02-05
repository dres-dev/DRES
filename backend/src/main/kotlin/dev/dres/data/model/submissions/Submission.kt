package dev.dres.data.model.submissions

import dev.dres.data.model.admin.User
import dev.dres.data.model.template.team.Team

typealias SubmissionId = String

interface Submission {

    val submissionId: SubmissionId
    val timestamp: Long
    val team: Team
    val user: User

    fun answerSets(): Sequence<AnswerSet>

}
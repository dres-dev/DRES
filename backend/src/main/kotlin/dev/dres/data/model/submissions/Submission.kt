package dev.dres.data.model.submissions

import dev.dres.data.model.admin.UserId
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.template.team.TeamId

typealias SubmissionId = String

interface Submission {

    val submissionId: SubmissionId
    val timestamp: Long
    val teamId: TeamId
    val memberId: UserId
    val evaluationId: EvaluationId

    fun answerSets(): Sequence<AnswerSet>
}
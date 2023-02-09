package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.admin.User
import dev.dres.data.model.submissions.AnswerSet
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionId
import dev.dres.data.model.template.team.Team

/**
 * The RESTful API equivalent of a [DbSubmission].
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
data class ApiSubmission(
    val id: SubmissionId,
    val teamId: String,
    val teamName: String,
    val memberId: String,
    val memberName: String,
    override val timestamp: Long,
    val answers: List<ApiAnswerSet>,
) : Submission {

    override val submissionId: SubmissionId
        get() = TODO("Not yet implemented")
    override val team: Team
        get() = TODO("Not yet implemented")
    override val user: User
        get() = TODO("Not yet implemented")

    override fun answerSets(): Sequence<AnswerSet> = this.answers.asSequence()
}

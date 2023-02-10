package dev.dres.api.rest.types.evaluation

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.data.model.admin.User
import dev.dres.data.model.submissions.AnswerSet
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionId
import dev.dres.data.model.template.team.Team
import java.util.UUID

/**
 * The RESTful API equivalent of a [DbSubmission].
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
data class ApiSubmission(
    override val teamId: String,
    val teamName: String,
    override val memberId: String,
    val memberName: String,
    override val timestamp: Long,
    val answers: List<ApiAnswerSet>,
    override val submissionId: SubmissionId = UUID.randomUUID().toString() //TODO is there a use case where this needs to be settable via an API request?
) : Submission {


    override fun answerSets(): Sequence<AnswerSet> = this.answers.asSequence()
    override fun toDb(): DbSubmission {
        TODO("Not yet implemented")
    }
}

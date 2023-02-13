package dev.dres.api.rest.types.evaluation

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.admin.User
import dev.dres.data.model.submissions.AnswerSet
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionId
import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.template.team.Team
import kotlinx.dnq.query.addAll
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.first
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
    val answers: List<ApiAnswerSet>,
    override val timestamp: Long = System.currentTimeMillis(),
    override val submissionId: SubmissionId = UUID.randomUUID().toString() //TODO is there a use case where this needs to be settable via an API request?
) : Submission {

    init {
        answers.forEach {
            it.submission = this
        }
    }

    override fun answerSets(): Sequence<AnswerSet> = this.answers.asSequence()
    override fun toDb(): DbSubmission {

        return DbSubmission.new {
            this.id = this@ApiSubmission.submissionId
            this.timestamp = this@ApiSubmission.timestamp
            this.team = DbTeam.filter { teamId eq this@ApiSubmission.teamId }.first()
            this.user = DbUser.filter { id eq this@ApiSubmission.memberId }.first()
            this.answerSets.addAll(
                this@ApiSubmission.answers.map { it.toDb() }
            )
        }

    }
}

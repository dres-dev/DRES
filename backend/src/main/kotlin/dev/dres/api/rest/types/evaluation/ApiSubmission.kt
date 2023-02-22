package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.submissions.*
import dev.dres.data.model.template.team.DbTeam
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
    override val submissionId: SubmissionId = UUID.randomUUID().toString(), //TODO is there a use case where this needs to be settable via an API request?
    override val teamId: String,
    override val memberId: String,
    val teamName: String,
    val memberName: String,
    val answers: List<ApiAnswerSet>,
    override val timestamp: Long = System.currentTimeMillis(),
    override val evaluationId: EvaluationId
) : Submission {

    init {
        answers.forEach {
            it.submission = this
        }
    }

    override fun answerSets(): Sequence<AnswerSet> = this.answers.asSequence()

    /**
     * Creates a new [DbSubmission] for this [ApiSubmission]. Requires an ongoing transaction.
     *
     * @return [DbSubmission]
     */
    fun toNewDb(): DbSubmission = DbSubmission.new {
        this.id = this@ApiSubmission.submissionId
        this.timestamp = this@ApiSubmission.timestamp
        this.team = DbTeam.filter { it.id eq this@ApiSubmission.teamId }.first()
        this.user = DbUser.filter { it.id eq this@ApiSubmission.memberId }.first()
        this.answerSets.addAll(
            this@ApiSubmission.answers.map { it.toNewDb() }
        )
    }
}

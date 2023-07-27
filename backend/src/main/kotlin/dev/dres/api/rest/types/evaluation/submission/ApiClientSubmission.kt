package dev.dres.api.rest.types.evaluation.submission

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.SubmissionId
import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.exceptions.IllegalTeamIdException
import io.javalin.openapi.OpenApiIgnore
import kotlinx.dnq.query.addAll
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.single
import kotlinx.dnq.query.singleOrNull
import java.util.UUID

/**
 * The RESTful API equivalent of a submission as provided by a client of the submission API endpoints.
 *
 * There is an inherent asymmetry between the submissions received by DRES (unprocessed & validated) and those sent by DRES (processed and validated).
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
 class ApiClientSubmission(val answerSets: List<ApiClientAnswerSet>) {

    /** The [UserId] associated with the submission. Is usually added as contextual information by the receiving endpoint. */
    @JsonIgnore
    @get:OpenApiIgnore
    @set:OpenApiIgnore
    var userId: UserId? = null

    /** The [TeamId] associated with the submission. Is usually added as contextual information by the receiving endpoint. */
    @JsonIgnore
    @get:OpenApiIgnore
    @set:OpenApiIgnore
    var teamId: TeamId? = null

    /** The [SubmissionId] of this [ApiClientSubmission]. Typically generated by the receiving endpoint. */
    @JsonIgnore
    @get:OpenApiIgnore
    val submissionId: SubmissionId = UUID.randomUUID().toString()

    /** The timestamp at which this [ApiClientSubmission] was received. Typically generated by the receiving endpoint.*/
    @JsonIgnore
    @get:OpenApiIgnore
    val timestamp: Long = System.currentTimeMillis()

    /**
     * Creates a new [DbSubmission] for this [ApiClientSubmission].
     *
     * Requires an ongoing transaction.
     *
     * @return [DbSubmission]
     */
    fun toNewDb(): DbSubmission = DbSubmission.new {
        this.id = this@ApiClientSubmission.submissionId
        this.timestamp = this@ApiClientSubmission.timestamp
        this.team = this@ApiClientSubmission.teamId?.let { teamId -> DbTeam.filter { it.id eq teamId }.singleOrNull() }
            ?: throw IllegalArgumentException("Failed to lookup specified team ID ${this@ApiClientSubmission.teamId}.")

        this.user = this@ApiClientSubmission.userId?.let { userId -> DbUser.filter { it.id eq userId }.singleOrNull() }
            ?: throw IllegalArgumentException("Failed to lookup specified user ID ${this@ApiClientSubmission.userId}.")
        this.answerSets.addAll(this@ApiClientSubmission.answerSets.map { it.toNewDb() })
    }
}
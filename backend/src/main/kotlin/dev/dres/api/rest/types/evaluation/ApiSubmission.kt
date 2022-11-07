package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionId

/**
 * The RESTful API equivalent of a [Submission].
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
    val timestamp: Long,
    val verdicts: List<ApiVerdict>,
)
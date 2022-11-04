package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionId
import dev.dres.data.model.submissions.SubmissionStatus

/**
 * Contains information about a [Submission].
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
data class ApiSubmission(
    val id: SubmissionId,
    val teamId: String? = null,
    val teamName: String? = null,
    val memberId: String? = null,
    val memberName: String? = null,
    val status: SubmissionStatus,
    val timestamp: Long? = null,
    val item: ApiMediaItem? = null,
    val text: String? = null,
    val start: Long? = null,
    val end: Long? = null
)
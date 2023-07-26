package dev.dres.api.rest.types.evaluation.submission


import dev.dres.data.model.submissions.*

/**
 * The RESTful API equivalent of a submission as returned by the DRES endpoint.
 *
 * There is an inherent asymmetry between the submissions received by DRES (unprocessed & validated) and those sent by DRES (processed and validated).
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
data class ApiSubmission(
    val submissionId: SubmissionId,
    val teamId: String,
    val memberId: String,
    val teamName: String,
    val memberName: String,
    val timestamp: Long,
    val answers: List<ApiAnswerSet> = emptyList()
 )
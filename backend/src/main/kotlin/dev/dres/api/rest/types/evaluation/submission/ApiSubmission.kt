package dev.dres.api.rest.types.evaluation.submission


import dev.dres.data.model.submissions.*
import kotlinx.serialization.Serializable

/**
 * The RESTful API equivalent of a submission as returned by the DRES endpoint.
 *
 * There is an inherent asymmetry between the submissions received by DRES (unprocessed & validated) and those sent by DRES (processed and validated).
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
@Serializable
data class ApiSubmission(
    override val submissionId: SubmissionId,
    override val teamId: String,
    override val memberId: String,
    val teamName: String,
    val memberName: String,
    override val timestamp: Long,
    val answers: List<ApiAnswerSet> = emptyList()
 ) : Submission {

    init {
        answers.forEach { it.submission = this }
    }
    override fun answerSets(): Sequence<AnswerSet> = answers.asSequence()

}
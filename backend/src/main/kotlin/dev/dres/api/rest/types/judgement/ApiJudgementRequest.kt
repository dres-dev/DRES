package dev.dres.api.rest.types.judgement

import dev.dres.api.rest.types.evaluation.submission.ApiAnswerSet

/**
 *
 * DTO for judgement requests:
 * A submission that has to be judged by the client.
 *
 * @author Loris Sauter
 * @author Ralph Gasser
 * @version 2.0
 */
class ApiJudgementRequest(
    val token: String?,
    val validator: String,
    val taskDescription: String,
    val answerSet: ApiAnswerSet
)

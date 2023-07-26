package dev.dres.api.rest.types.judgement

import dev.dres.api.rest.types.evaluation.submission.ApiVerdictStatus

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class ApiJudgement(val token: String, val validator: String, val verdict: ApiVerdictStatus)
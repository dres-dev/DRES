package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.evaluation.submission.ApiVerdictStatus
import kotlinx.serialization.Serializable

/**
 * Data transfer object for overriding a [ApiAnswerSet]'s verdict.
 *
 * @see ApiAnswerSet
 * @author Loris Sauter
 * @version 1.0
 */
@Serializable
data class ApiOverrideAnswerSetVerdictDto(val verdict: ApiVerdictStatus)

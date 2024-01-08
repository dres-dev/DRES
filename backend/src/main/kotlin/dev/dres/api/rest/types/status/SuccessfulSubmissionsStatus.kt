package dev.dres.api.rest.types.status

import dev.dres.api.rest.types.evaluation.submission.ApiVerdictStatus
import kotlinx.serialization.Serializable

@Serializable
data class SuccessfulSubmissionsStatus(val submission: ApiVerdictStatus, val description: String) : AbstractStatus(status = true)

package dev.dres.api.rest.types.status

import dev.dres.api.rest.types.evaluation.ApiVerdictStatus

data class SuccessfulSubmissionsStatus(val submission: ApiVerdictStatus, val description: String) : AbstractStatus(status = true)

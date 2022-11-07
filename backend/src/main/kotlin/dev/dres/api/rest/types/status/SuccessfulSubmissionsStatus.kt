package dev.dres.api.rest.types.status

import dev.dres.data.model.submissions.VerdictStatus

data class SuccessfulSubmissionsStatus(val submission: VerdictStatus, val description: String) : AbstractStatus(status = true)
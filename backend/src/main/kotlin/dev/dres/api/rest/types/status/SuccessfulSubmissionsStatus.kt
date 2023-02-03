package dev.dres.api.rest.types.status

import dev.dres.data.model.submissions.DbVerdictStatus

data class SuccessfulSubmissionsStatus(val submission: DbVerdictStatus, val description: String) : AbstractStatus(status = true)
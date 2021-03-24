package dev.dres.api.rest.types.status

import dev.dres.data.model.submissions.SubmissionStatus

data class SuccessfulSubmissionsStatus(val submission: SubmissionStatus, val description: String) : AbstractStatus(status = true)
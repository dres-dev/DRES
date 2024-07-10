package dev.dres.api.rest.types.evaluation.submission

import kotlinx.serialization.Serializable

@Serializable
data class ApiSubmissionList(val submissions: List<ApiSubmission>)
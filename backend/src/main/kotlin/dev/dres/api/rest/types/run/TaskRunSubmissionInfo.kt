package dev.dres.api.rest.types.run

data class TaskRunSubmissionInfo(
    val taskRunId: String,
    val submissions: List<SubmissionInfo>
)

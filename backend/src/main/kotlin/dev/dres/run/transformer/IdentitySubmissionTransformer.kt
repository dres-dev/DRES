package dev.dres.run.transformer

import dev.dres.api.rest.types.evaluation.ApiSubmission

object IdentitySubmissionTransformer : SubmissionTransformer {
    override fun transform(submission: ApiSubmission): ApiSubmission = submission
}
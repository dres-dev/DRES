package dev.dres.run.transformer

import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.data.model.submissions.Submission

interface SubmissionTransformer {

    fun transform(submission: ApiSubmission): ApiSubmission

}
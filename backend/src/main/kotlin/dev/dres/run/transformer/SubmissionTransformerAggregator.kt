package dev.dres.run.transformer

import dev.dres.api.rest.types.evaluation.ApiSubmission

class SubmissionTransformerAggregator(private val transformers: List<SubmissionTransformer>) : SubmissionTransformer {

    override fun transform(submission: ApiSubmission): ApiSubmission {

        var transformed = submission
        for (transformer in transformers) {
            transformed = transformer.transform(transformed)
        }
        return transformed

    }
}
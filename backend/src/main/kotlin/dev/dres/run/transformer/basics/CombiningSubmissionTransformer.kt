package dev.dres.run.transformer.basics

import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.data.model.submissions.DbSubmission

/**
 * A [SubmissionTransformer] for [DbSubmission]s that combines multiple [SubmissionTransformer].
 *
 * @author Luca Rossetto
 */
class CombiningSubmissionTransformer(private val transformers: List<SubmissionTransformer>) : SubmissionTransformer {

    /**
     * Apply this [CombiningSubmissionTransformer] to the provided [DbSubmission]. Transformation happens in place.
     *
     * Requires an ongoing transaction if nested [SubmissionTransformer]s require an ongoing transaction.
     *
     * @param submission [DbSubmission] to transform.
     */
    override fun transform(submission: ApiClientSubmission) : ApiClientSubmission {
        var sub = submission
        for (transformer in transformers) {
            sub = transformer.transform(sub)
        }
        return sub
    }
}
package dev.dres.run.transformer.basics

import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.data.model.submissions.DbSubmission


/**
 * A transformer for [DbSubmission]s.
 *
 * @author Luca Rossetto
 */
interface SubmissionTransformer {

    /**
     * Apply this [SubmissionTransformer] to the provided [ApiClientSubmission].
     *
     * Usually requires an ongoing transaction.
     *
     * @param submission [ApiClientSubmission] to transform.
     */
    fun transform(submission: ApiClientSubmission) : ApiClientSubmission

}
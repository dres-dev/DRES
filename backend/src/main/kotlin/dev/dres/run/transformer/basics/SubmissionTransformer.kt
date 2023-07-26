package dev.dres.run.transformer.basics

import dev.dres.data.model.submissions.DbSubmission


/**
 * A transformer for [DbSubmission]s.
 *
 * @author Luca Rossetto
 */
interface SubmissionTransformer {

    /**
     * Apply this [SubmissionTransformer] to the provided [DbSubmission]. Transformation happens in place.
     *
     * Usually requires an ongoing transaction.
     *
     * @param submission [DbSubmission] to transform.
     */
    fun transform(submission: DbSubmission)

}
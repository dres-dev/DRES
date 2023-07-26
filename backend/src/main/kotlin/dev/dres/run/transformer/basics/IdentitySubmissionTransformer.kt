package dev.dres.run.transformer.basics

import dev.dres.data.model.submissions.DbSubmission

/**
 * A [SubmissionTransformer] that does not make any changes.
 *
 * @author Luca Rossetto
 */
object IdentitySubmissionTransformer : SubmissionTransformer {
    /**
     * Apply this [IdentitySubmissionTransformer] to the provided [DbSubmission].
     *
     * @param submission [DbSubmission] to transform.
     */
    override fun transform(submission: DbSubmission) {
        /* No op. */
    }
}
package dev.dres.run.validation.interfaces

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus

interface SubmissionJudgementValidator : SubmissionValidator, JudgementValidator {

    /**
     * Enqueues a [Submission] with the internal judgment queue and updates its [SubmissionStatus]
     * to [SubmissionStatus.INDETERMINATE].
     *
     * @param submission The [Submission] to validate.
     */
    override fun validate(submission: Submission)
    override val deferring: Boolean
        get() = true

}
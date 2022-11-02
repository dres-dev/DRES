package dev.dres.run.validation

import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.run.validation.interfaces.SubmissionValidator

/**
 * A [SubmissionValidator] that checks if the items specified by a [Submission] match the items in the provided set.
 *
 * @author Luca Rossetto
 * @version 1.0.1
 */
class MediaItemsSubmissionValidator(private val items : Set<MediaItem>) : SubmissionValidator {

    /** This type of [SubmissionValidator] can be executed directly.*/
    override val deferring = false

    /**
     * Performs the validation.
     *
     * @param submission The [Submission] to validate.
     */
    override fun validate(submission: Submission) {
        if (submission.item == null || submission.item !in this.items) {
            submission.status = SubmissionStatus.WRONG
        } else {
            submission.status = SubmissionStatus.CORRECT
        }
    }
}
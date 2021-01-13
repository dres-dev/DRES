package dev.dres.run.validation

import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.run.validation.interfaces.SubmissionValidator

class MediaItemsSubmissionValidator(private val items : Set<MediaItem>) : SubmissionValidator {

    override fun validate(submission: Submission) {
        submission.status = when (submission.item) {
            in items -> SubmissionStatus.CORRECT
            else -> SubmissionStatus.WRONG
        }
    }

    override val deferring: Boolean
        get() = false


}
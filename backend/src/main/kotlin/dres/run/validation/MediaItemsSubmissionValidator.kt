package dres.run.validation

import dres.data.model.basics.media.MediaItem
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import dres.run.validation.interfaces.SubmissionValidator

class MediaItemsSubmissionValidator(private val items : Set<MediaItem>) : SubmissionValidator {

    override fun validate(submission: Submission) {
        submission.status = when (submission.item) {
            in items -> SubmissionStatus.CORRECT
            else -> SubmissionStatus.WRONG
        }
    }


}
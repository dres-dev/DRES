package dev.dres.run.validation

import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.aspects.ItemAspect
import dev.dres.run.validation.interfaces.SubmissionValidator

class MediaItemsSubmissionValidator(private val items : Set<MediaItem>) : SubmissionValidator {

    override fun validate(submission: Submission) {

        if (submission !is ItemAspect) {
            submission.status = SubmissionStatus.WRONG
            return
        }

        submission.status = when (submission.item) {
            in items -> SubmissionStatus.CORRECT
            else -> SubmissionStatus.WRONG
        }
    }

    override val deferring = false


}
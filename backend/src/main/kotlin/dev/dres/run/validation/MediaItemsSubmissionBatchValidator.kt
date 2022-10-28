package dev.dres.run.validation

import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.batch.ResultBatch
import dev.dres.run.validation.interfaces.SubmissionBatchValidator

class MediaItemsSubmissionBatchValidator(private val items : Set<MediaItem>) : SubmissionBatchValidator {

    override fun validate(batch: ResultBatch<*>) {

        batch.results.forEach {
            it.status = if (it.item in items) SubmissionStatus.CORRECT else SubmissionStatus.WRONG
        }

    }
}
package dev.dres.run.validation.judged

import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.TemporalSubmissionAspect

/** Helper class to store submission information independent of source */
data class ItemRange(val item: MediaItem, val start: Long, val end: Long){
    constructor(submission: TemporalSubmissionAspect): this(submission.item, submission.start, submission.end)
    constructor(submission: Submission): this(submission.item, 0, 0)
}
package dev.dres.data.model.submissions.batch

import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.aspects.TemporalAspect

/**
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
data class TemporalBatchElement(override val item: MediaItem, override val start: Long, override val end: Long, ) : BaseResultBatchElement, TemporalAspect {
    override var status: SubmissionStatus = SubmissionStatus.INDETERMINATE
    override val temporalRange: TemporalRange
        get() = TemporalRange(TemporalPoint.Millisecond(start), TemporalPoint.Millisecond(end))
}
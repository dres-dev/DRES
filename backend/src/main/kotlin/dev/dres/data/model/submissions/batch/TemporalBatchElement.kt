package dev.dres.data.model.submissions.batch

import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.basics.time.MilliSecondTemporalPoint
import dev.dres.data.model.basics.time.TemporalRange
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
        get() = TemporalRange(MilliSecondTemporalPoint(start), MilliSecondTemporalPoint(end))
}
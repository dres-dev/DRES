package dev.dres.data.model.submissions.aspects

import dev.dres.data.model.media.time.TemporalRange

/**
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
interface TemporalAspect {

    /** Start time in milliseconds */
    val start: Long

    /** End time in milliseconds */
    val end: Long

    val temporalRange: TemporalRange
}

package dev.dres.data.model.submissions

import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange
/**
 * A [Answer] as issued by a DRES user as part of a [AnswerSet].
 *
 * This abstraction is mainly required to enable testability of implementations.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
interface Answer {

    val item: MediaItem?
    val start: Long?
    val end: Long?
    val text: String?

    /**  Returns the [TemporalRange] for this [DbAnswerSet]. */
    val temporalRange: TemporalRange?
        get() {
            val start = this.start ?: return null
            val end = this.end ?: return null
            return TemporalRange(TemporalPoint.Millisecond(start), TemporalPoint.Millisecond(end))
        }

    /**
     *
     */
    fun type(): AnswerType
}
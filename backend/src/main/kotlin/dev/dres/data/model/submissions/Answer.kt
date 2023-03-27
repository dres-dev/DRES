package dev.dres.data.model.submissions

import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange

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

    fun type(): AnswerType

    infix fun eq(answer: Answer) : Boolean =
        answer.type() == this.type() &&
                answer.item?.mediaItemId == this.item?.mediaItemId &&
                //answer.item?.collection?.id == this.item?.collection?.id &&
                answer.start == this.start &&
                answer.end == this.end &&
                answer.text == this.text
}
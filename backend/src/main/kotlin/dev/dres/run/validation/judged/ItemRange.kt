package dev.dres.run.validation.judged

import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.Verdict
import dev.dres.data.model.submissions.VerdictType

/**
 * Helper class to store submission information independent of source.
 *
 * @author Luca Rossetto
 * @version 2.0.0
 */
data class ItemRange(val element: String, val start: Long, val end: Long){
    constructor(item: MediaItem): this(item.id, 0, 0)
    constructor(item: MediaItem, start: Long, end: Long): this(item.id, start, end)
    constructor(verdict: Verdict): this(when (verdict.type){
            VerdictType.ITEM,
            VerdictType.TEMPORAL -> verdict.item!!.id
            VerdictType.TEXT  -> verdict.text!!
            else -> throw IllegalStateException("Submission contains neither item nor text.")
        }, verdict.start ?: 0, verdict.end ?: 0)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ItemRange

        if (element != other.element) return false
        if (start != other.start) return false
        if (end != other.end) return false

        return true
    }

    override fun hashCode(): Int {
        var result = element.hashCode()
        result = 31 * result + start.hashCode()
        result = 31 * result + end.hashCode()
        return result
    }
}
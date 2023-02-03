package dev.dres.run.validation.judged

import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.DbAnswerType

/**
 * Helper class to store submission information independent of source.
 *
 * @author Luca Rossetto
 * @version 2.0.0
 */
data class ItemRange(val element: String, val start: Long, val end: Long){
    constructor(item: DbMediaItem): this(item.id, 0, 0)
    constructor(item: DbMediaItem, start: Long, end: Long): this(item.id, start, end)
    constructor(answerSet: DbAnswerSet): this(when (answerSet.type){
            DbAnswerType.ITEM,
            DbAnswerType.TEMPORAL -> answerSet.item!!.id
            DbAnswerType.TEXT  -> answerSet.text!!
            else -> throw IllegalStateException("Submission contains neither item nor text.")
        }, answerSet.start ?: 0, answerSet.end ?: 0)

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
package dev.dres.run.validation.judged

import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.submissions.Answer
import dev.dres.data.model.submissions.AnswerType
import dev.dres.data.model.submissions.DbAnswer
import dev.dres.data.model.submissions.DbAnswerType

/**
 * Helper class to store submission information independent of source.
 *
 * @author Luca Rossetto
 * @version 2.0.0
 */
data class ItemRange(val element: String, val start: Long, val end: Long) {
    constructor(item: DbMediaItem): this(item.id, 0, 0)
    constructor(item: DbMediaItem, start: Long, end: Long): this(item.id, start, end)
    constructor(answer: Answer): this(when (answer.type()){
            AnswerType.ITEM,
            AnswerType.TEMPORAL -> answer.item!!.mediaItemId
            AnswerType.TEXT  -> answer.text!!
        }, answer.start ?: 0, answer.end ?: 0)
}
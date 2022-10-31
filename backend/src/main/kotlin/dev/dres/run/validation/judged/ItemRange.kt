package dev.dres.run.validation.judged

import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.aspects.ItemAspect
import dev.dres.data.model.submissions.aspects.TemporalSubmissionAspect
import dev.dres.data.model.submissions.aspects.TextAspect

/** Helper class to store submission information independent of source */
data class ItemRange(val element: String, val start: Long, val end: Long){
    constructor(submission: TemporalSubmissionAspect): this(submission.item.id, submission.start, submission.end)
    constructor(submission: TextAspect): this(submission.text, 0, 0)
    constructor(submission: ItemAspect): this(submission.item)
    constructor(item: MediaItem): this(item.id, 0, 0)
    constructor(item: MediaItem, start: Long, end: Long): this(item.id, start, end)
    constructor(submission: Submission): this(fromSubmission(submission),
        if (submission is TemporalSubmissionAspect) submission.start else 0,
        if (submission is TemporalSubmissionAspect) submission.end else 0)

    companion object {
        private fun fromSubmission(submission: Submission): String {
            return when (submission){
                is ItemAspect -> submission.item.id
                is TextAspect -> submission.text
                else -> throw IllegalStateException("Submission contains neither item nor text")
            }

        }
    }

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
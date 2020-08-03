package dres.data.model.competition

import dres.data.model.basics.media.MediaItem
import dres.data.model.basics.time.TemporalRange


/**
 * Represents the target of a [TaskDescription], i.e., the media object or segment that is
 * considered correct.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
sealed class TaskDescriptionTarget {

    internal abstract fun textDescription(): String

    /**
     * A [TaskDescriptionTarget] that is validated by human judges.
     */
    object JudgementTaskDescriptionTarget : TaskDescriptionTarget() {
        override fun textDescription() = "Judgement"
    }

    /**
     * A  [TaskDescriptionTarget], specified by a [MediaItem].
     */
    data class MediaItemTarget(val item: MediaItem) : TaskDescriptionTarget() {
        override fun textDescription() = "Media Item ${item.name}"
    }

    /**
     * A video segment [TaskDescriptionTarget], specified by a [MediaItem.VideoItem] and a [TemporalRange].
     */
    data class MediaSegmentTarget(override val item: MediaItem.VideoItem, override val temporalRange: TemporalRange) : TaskDescriptionTarget(), CachedVideoItem {
        override fun textDescription() = "Media Item ${item.name} @ ${temporalRange.start.niceText()} - ${temporalRange.end.niceText()}"
    }
}
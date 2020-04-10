package dres.data.model.competition.interfaces

import dres.data.model.basics.MediaItem
import dres.data.model.basics.TemporalRange

/**
 * A [TaskDescription] looking for a specific temporal segment in terms of a [MediaItem] and a [TemporalRange].
 *
 * @author Ralph Gasser
 * @version 1.0
 */
interface MediaSegmentTaskDescription : MediaItemTaskDescription {
    override val item: MediaItem.VideoItem
    val temporalRange: TemporalRange
}
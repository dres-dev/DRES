package dres.data.model.competition.interfaces

import dres.data.model.basics.media.MediaItem
import dres.data.model.basics.time.TemporalRange

/**
 * A [TaskDescription] looking for a specific temporal segment in terms of a [MediaItem] and a [TemporalRange].
 *
 * @author Ralph Gasser
 * @version 1.0
 */
interface MediaSegmentTaskDescription : MediaItemTaskDescription {
    override val item: MediaItem.VideoItem
    val temporalRange: TemporalRange

    /**
     * Returns the file name of the segment, used for pre-computing
     */
    fun cacheItemName(): String

}
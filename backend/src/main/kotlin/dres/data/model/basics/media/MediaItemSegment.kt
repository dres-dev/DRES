package dres.data.model.basics.media

import dres.data.model.Entity
import dres.data.model.basics.time.TemporalRange

/** TODO: Media segments have names? Shouldn't there be some kind of a segment number instead? */
data class MediaItemSegment(val mediaItemId: Long, val name: String, val range: TemporalRange)

data class MediaItemSegmentList(override var id: Long, val mediaItemId: Long, val segments: MutableList<MediaItemSegment>) : Entity {
    init {
        require(segments.all { it.mediaItemId == mediaItemId } ){"All segments need to belong to the same media item"}
    }
}
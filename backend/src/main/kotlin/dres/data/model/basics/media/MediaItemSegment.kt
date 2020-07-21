package dres.data.model.basics.media

import dres.data.model.Entity
import dres.data.model.UID
import dres.data.model.basics.time.TemporalRange

data class MediaItemSegment(val mediaItemId: UID, val name: String, val range: TemporalRange)

data class MediaItemSegmentList(override var id: UID, val mediaItemId: UID, val segments: MutableList<MediaItemSegment>) : Entity {
    init {
        require(segments.all { it.mediaItemId == mediaItemId } ){"All segments need to belong to the same media item"}
    }
}
package dres.data.model.basics.media

import dres.data.model.Entity
import dres.data.model.basics.time.TemporalRange

/** TODO: Media segments have names? Shouldn't there be some kind of a segment number instead? */
data class MediaItemSegment(override var id: Long, val mediaItemId: Long, val name: String, val range: TemporalRange) : Entity
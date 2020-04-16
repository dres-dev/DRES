package dres.data.model.basics

import dres.data.model.Entity

/** TODO: Media segments have names? Shouldn't there be some kind of a segment number instead? */
data class MediaItemSegment(override var id: Long, val mediaItemId: Long, val name: String, val range: TemporalRange) : Entity
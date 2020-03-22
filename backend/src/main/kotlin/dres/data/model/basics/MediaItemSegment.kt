package dres.data.model.basics

import dres.data.model.Entity

data class MediaItemSegment(override var id: Long, val mediaItemId: Long, val name: String, val range: TemporalRange) : Entity
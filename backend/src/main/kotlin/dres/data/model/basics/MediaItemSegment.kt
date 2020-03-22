package dres.data.model.basics

import dres.data.model.Entity

data class MediaItemSegment(override var id: Long, val mediaItemId: Long, val range: TemporalRange) : Entity
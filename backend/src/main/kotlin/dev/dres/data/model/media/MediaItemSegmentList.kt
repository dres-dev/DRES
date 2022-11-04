package dev.dres.data.model.media

import dev.dres.data.model.PersistentEntity

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class MediaItemSegmentList(override var id: EvaluationId, val mediaItemId: EvaluationId, val segments: MutableList<MediaSegment>) : PersistentEntity {
    init {
        require(segments.all { it.mediaItemId == mediaItemId } ){"All segments need to belong to the same media item"}
    }
}
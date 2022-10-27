package dev.dres.data.model.basics.media

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.UID

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class MediaItemSegmentList(override var id: UID, val mediaItemId: UID, val segments: MutableList<MediaItemSegment>) : PersistentEntity {
    init {
        require(segments.all { it.mediaItemId == mediaItemId } ){"All segments need to belong to the same media item"}
    }
}
package dev.dres.api.rest.types.collection

import dev.dres.data.model.UID
import dev.dres.data.model.media.MediaItem

/**
 * The RESTful API equivalent for [MediaItem].
 *
 * @see MediaItem
 * @author Ralph Gasser
 * @version 1.1.0
 */
data class ApiMediaItem(val id: String= UID.EMPTY.string, val name: String, val type: ApiMediaType, val collectionId: String, val location: String, val durationMs: Long? = null, val fps: Float? = null) {
    init {
        if (this.type == ApiMediaType.VIDEO) {
            require(this.durationMs != null) { "Duration must be set for a video item." }
            require(this.fps != null) { "Duration must be set for a video item." }
        }
    }
}
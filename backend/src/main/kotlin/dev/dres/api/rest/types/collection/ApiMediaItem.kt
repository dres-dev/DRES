package dev.dres.api.rest.types.collection

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.MediaItemCollection
import dev.dres.data.model.media.MediaItemType

/**
 * The RESTful API equivalent for [DbMediaItem].
 *
 * @see DbMediaItem
 * @author Ralph Gasser
 * @version 1.1.0
 */
data class ApiMediaItem(
    override val id: String?,
    override val name: String,
    val type: ApiMediaType,
    val collectionId: String,
    val location: String,
    val durationMs: Long? = null,
    val fps: Float? = null
) : MediaItem {
    init {
        if (this.type == ApiMediaType.VIDEO) {
            require(this.durationMs != null) { "Duration must be set for a video item." }
            require(this.fps != null) { "Duration must be set for a video item." }
        }
    }

    override val collection: MediaItemCollection //TODO do we want this here?
        @JsonIgnore
        get() = TODO("Not yet implemented")

    override fun type(): MediaItemType = MediaItemType.fromApi(this.type)
}
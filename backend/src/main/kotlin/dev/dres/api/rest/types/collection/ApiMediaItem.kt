package dev.dres.api.rest.types.collection

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.data.model.media.*
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.first
import kotlinx.dnq.util.findById

/**
 * The RESTful API equivalent for [DbMediaItem].
 *
 * @see DbMediaItem
 * @author Ralph Gasser
 * @version 1.1.0
 */
data class ApiMediaItem(
    override val mediaItemId: MediaItemId,
    override val name: String,
    val type: ApiMediaType,
    val collectionId: String,
    val location: String,
    val durationMs: Long? = null,
    val fps: Float? = null,
    val metadata: List<ApiMediaItemMetaDataEntry>
) : MediaItem {
    init {
        if (this.type == ApiMediaType.VIDEO) {
            require(this.durationMs != null) { "Duration must be set for a video item." }
            require(this.fps != null) { "Duration must be set for a video item." }
        }
    }

    override fun dbCollection(): DbMediaCollection {
        return DbMediaCollection.filter { it.id eq collectionId }.first()
    }

    override fun type(): MediaItemType = MediaItemType.fromApi(this.type)
}
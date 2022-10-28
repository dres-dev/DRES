package dev.dres.api.rest.types.collection

import dev.dres.data.model.media.MediaType

/**
 * The RESTful API equivalent for the type of a [ApiMediaItem]
 *
 * @see ApiMediaItem
 * @author Ralph Gasser
 * @version 1.0
 */
enum class ApiMediaType(val mediaType: MediaType) {
    IMAGE(MediaType.IMAGE),
    VIDEO(MediaType.VIDEO)
}
package dev.dres.api.rest.types.collection

import dev.dres.data.model.media.MediaType

/**
 * The RESTful API equivalent for the type of a [ApiMediaItem]
 *
 * @see ApiMediaItem
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiMediaType {
    IMAGE, VIDEO, TEXT;

    /**
     * Converts this [ApiMediaType] to a [MediaType] representation. Requires an ongoing transaction!
     *
     * @return [MediaType]
     */
    fun toMediaType(): MediaType = when(this) {
        IMAGE -> MediaType.IMAGE
        VIDEO -> MediaType.VIDEO
        TEXT -> MediaType.TEXT
    }
}
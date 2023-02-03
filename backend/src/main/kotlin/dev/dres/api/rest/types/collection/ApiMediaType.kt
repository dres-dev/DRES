package dev.dres.api.rest.types.collection

import dev.dres.data.model.media.DbMediaType

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
     * Converts this [ApiMediaType] to a [DbMediaType] representation. Requires an ongoing transaction!
     *
     * @return [DbMediaType]
     */
    fun toDb(): DbMediaType = when(this) {
        IMAGE -> DbMediaType.IMAGE
        VIDEO -> DbMediaType.VIDEO
        TEXT -> DbMediaType.TEXT
    }
}
package dev.dres.data.model.media

import dev.dres.api.rest.types.collection.ApiMediaType
import dev.dres.api.rest.types.competition.ApiTeam
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * A persistable media type enumeration such as a video or an image
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class MediaType(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<MediaType>() {
        val IMAGE by enumField { description = "IMAGE"; suffix = "mp4"; }
        val VIDEO by enumField { description = "VIDEO"; suffix = "jpg"; }
    }

    var description by xdRequiredStringProp(unique = true)
        private set

    /** The default suffix used for this [MediaType]. */
    var suffix by xdRequiredStringProp(unique = true)

    /**
     * Converts this [MediaType] to a RESTful API representation [ApiMediaType].
     *
     * This is a convenience method and requires an active transaction context.
     */
    fun toApi(): ApiMediaType
        = ApiMediaType.values().find { it.mediaType == this } ?: throw IllegalStateException("Media type ${this.description} is not supported.")
}

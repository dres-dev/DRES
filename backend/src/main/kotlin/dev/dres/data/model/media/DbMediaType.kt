package dev.dres.data.model.media

import dev.dres.api.rest.types.collection.ApiMediaType
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
class DbMediaType(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbMediaType>() {
        val IMAGE by enumField { description = "IMAGE"; suffix = "mp4"; }
        val VIDEO by enumField { description = "VIDEO"; suffix = "jpg"; }
        val TEXT by enumField { description = "TEXT"; suffix = "txt"; }
    }

    var description by xdRequiredStringProp(unique = true)
        private set

    /** The default suffix used for this [DbMediaType]. */
    var suffix by xdRequiredStringProp(unique = true)

    /**
     * Converts this [DbMediaType] to a RESTful API representation [ApiMediaType].
     *
     * This is a convenience method and requires an active transaction context.
     */
    fun toApi(): ApiMediaType
        = ApiMediaType.values().find { it.toDb() == this } ?: throw IllegalStateException("Media type ${this.description} is not supported.")

    override fun toString(): String = this.description
}

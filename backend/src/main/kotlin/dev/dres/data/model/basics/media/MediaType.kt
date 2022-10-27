package dev.dres.data.model.basics.media

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
        val IMAGE by enumField { description = "IMAGE" }
        val VIDEO by enumField { description = "VIDEO" }
    }

    var description by xdRequiredStringProp(unique = true)
        private set
}

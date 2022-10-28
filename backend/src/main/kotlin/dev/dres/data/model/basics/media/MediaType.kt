package dev.dres.data.model.basics.media

import dev.dres.api.rest.ApiRole
import dev.dres.api.rest.types.collection.RestMediaItem
import dev.dres.api.rest.types.collection.RestMediaItemType
import dev.dres.data.model.admin.Role
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

        /**
         * Generates and returns the [MediaType] that corresponds to the given [RestMediaItemType].
         *
         * @param type [RestMediaItemType]
         */
        fun convertApiType(type: RestMediaItemType): MediaType = when(type) {
            RestMediaItemType.IMAGE -> MediaType.IMAGE
            RestMediaItemType.VIDEO -> MediaType.VIDEO
        }
    }

    var description by xdRequiredStringProp(unique = true)
        private set
}

package dev.dres.data.model.basics.media

import dev.dres.data.model.PersistentEntity
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.xdRequiredStringProp
import kotlinx.dnq.xdStringProp

/**
 * A named media collection consisting of media items.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class MediaCollection(entity: Entity): PersistentEntity(entity) {
    companion object : XdNaturalEntityType<MediaCollection>()
    val name: String by xdRequiredStringProp(unique = true, trimmed = false)
    val path: String by xdRequiredStringProp(unique = true, trimmed = false)
    val description: String? by xdStringProp(trimmed = false)
}



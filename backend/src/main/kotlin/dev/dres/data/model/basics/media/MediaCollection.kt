package dev.dres.data.model.basics.media

import dev.dres.data.model.PersistentEntity
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

typealias CollectionId = String

/**
 * A named media collection consisting of media items.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class MediaCollection(entity: Entity): PersistentEntity(entity) {
    companion object : XdNaturalEntityType<MediaCollection>()
    var name: String by xdRequiredStringProp(unique = true, trimmed = false)
    var path: String by xdRequiredStringProp(unique = true, trimmed = false)
    var description: String? by xdStringProp(trimmed = false)
    val items by xdChildren0_N(MediaItem::collection)
}



package dev.dres.data.model.media

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
    /** The name of this [MediaItem]. */
    var name: String by xdRequiredStringProp(unique = true, trimmed = false)

    /** The path to the folder containing [MediaItem]s in this [MediaCollection]. */
    var path: String by xdRequiredStringProp(unique = true, trimmed = false)

    /** A textual description of this [MediaCollection]. */
    var description: String? by xdStringProp(trimmed = false)

    /** A list of [MediaItem]s in this [MediaCollection]. */
    val items by xdChildren0_N<MediaCollection, MediaItem>(MediaItem::collection)
}



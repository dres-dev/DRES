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
class DbMediaCollection(entity: Entity): PersistentEntity(entity) {
    companion object : XdNaturalEntityType<DbMediaCollection>()
    /** The name of this [DbMediaItem]. */
    var name: String by xdRequiredStringProp(unique = true, trimmed = false)

    /** The path to the folder containing [DbMediaItem]s in this [DbMediaCollection]. */
    var path: String by xdRequiredStringProp(unique = true, trimmed = false)

    /** A textual description of this [DbMediaCollection]. */
    var description: String? by xdStringProp(trimmed = false)

    /** A list of [DbMediaItem]s in this [DbMediaCollection]. */
    val items by xdChildren0_N<DbMediaCollection, DbMediaItem>(DbMediaItem::collection)
}



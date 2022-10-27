package dev.dres.data.model

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEntity
import kotlinx.dnq.xdRequiredStringProp

/**
 *
 */
abstract class PersistentEntity(entity: Entity): XdEntity(entity) {
    /** */
    var id: String by xdRequiredStringProp(unique = true, trimmed = false)
}
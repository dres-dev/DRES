package dev.dres.data.model

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEntity
import kotlinx.dnq.xdRequiredStringProp

/**
 * The root class for all DRES entities that are persisted via Xodus DNQ.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
abstract class PersistentEntity(entity: Entity): XdEntity(entity) {
    var id: String by xdRequiredStringProp(unique = true, trimmed = false)
}
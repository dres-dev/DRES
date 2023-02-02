package dev.dres.data.model

import dev.dres.data.model.template.task.TaskTemplate
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEntity
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.xdRequiredStringProp
import java.util.*

/**
 * The root class for all DRES entities that are persisted via Xodus DNQ.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
abstract class PersistentEntity(entity: Entity): XdEntity(entity) {
    companion object: XdNaturalEntityType<PersistentEntity>()
    var id: String by xdRequiredStringProp(unique = true, trimmed = false)

    override fun constructor() {
        this.id = UUID.randomUUID().toString()
    }
}
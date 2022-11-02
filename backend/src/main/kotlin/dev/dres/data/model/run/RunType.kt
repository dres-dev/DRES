package dev.dres.data.model.run

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * Enumeration of the type of [Evaluation].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class RunType(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<RunType>() {
        val INTERACTIVE_SYNCHRONOUS by enumField { description = "INTERACTIVE_SYNCHRONOUS" }
        val INTERACTIVE_ASYNCHRONOUS by enumField { description = "INTERACTIVE_ASYNCHRONOUS" }
        val NON_INTERACTIVE by enumField { description = "NON_INTERACTIVE" }
    }

    var description by xdRequiredStringProp(unique = true)
        private set
}
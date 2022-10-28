package dev.dres.data.model.audit

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * Enumeration of the source of a [AuditLogEntry].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class AuditLogSource(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<AuditLogSource>() {
        val REST by enumField { description = "REST" }
        val CLI by enumField { description = "CLI" }
        val INTERNAL by enumField { description = "INTERNAL" }
    }

    /** Name / description of the [AuditLogType]. */
    var description by xdRequiredStringProp(unique = true)
        private set
}
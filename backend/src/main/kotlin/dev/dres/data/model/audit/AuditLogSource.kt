package dev.dres.data.model.audit

import dev.dres.api.rest.types.audit.ApiAuditLogSource
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

    /**
     * Converts this [AuditLogSource] to a RESTful API representation [ApiAuditLogSource].
     *
     * @return [ApiAuditLogSource]
     */
    fun toApi(): ApiAuditLogSource
        = ApiAuditLogSource.values().find { it.source == this } ?: throw IllegalStateException("Audit log source ${this.description} is not supported.")
}